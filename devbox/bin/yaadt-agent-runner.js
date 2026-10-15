const http = require('http');
const fs = require('fs');
const os = require('os');
const path = require('path');
const {spawn, execFileSync} = require('child_process');

const port = Number(process.env.YAADT_AGENT_RUNNER_PORT || 3010);
const token = process.env.YAADT_AGENT_RUNNER_TOKEN || '';
const timeoutMs = Number(process.env.YAADT_AGENT_RUNNER_TIMEOUT_MS || 1800000);
const opencodeBin = process.env.YAADT_OPENCODE_BIN || '/opt/opencode/node_modules/.bin/opencode';
const maxConcurrentRuns = Number(process.env.YAADT_AGENT_RUNNER_MAX_CONCURRENT || 2);
let activeRuns = 0;

function response(res, status, body) {
  res.writeHead(status, {'content-type': 'application/json'});
  res.end(JSON.stringify(body));
}

function checkpoint(cwd) {
  try {
    execFileSync('git', ['config', 'user.name', process.env.GIT_AUTHOR_NAME || 'YAADT Agent'], {cwd});
    execFileSync('git', ['config', 'user.email', process.env.GIT_AUTHOR_EMAIL || 'yaadt-agent@local'], {cwd});
    execFileSync('git', ['add', '-A'], {cwd});
    try {
      execFileSync('git', ['commit', '-m', 'YAADT agent checkpoint'], {cwd, encoding: 'utf8'});
    } catch (commitError) {
      if (!String(commitError.stdout || '').includes('nothing to commit')) throw commitError;
    }
    return execFileSync('git', ['rev-parse', 'HEAD'], {cwd, encoding: 'utf8'}).trim();
  } catch (error) {
    return null;
  }
}

function validateCommand(command, cwd, label) {
  if (!command || !command.trim()) return {ok: true, output: ''};
  try {
    const output = execFileSync('/bin/sh', ['-lc', command], {cwd, encoding: 'utf8', maxBuffer: 4 * 1024 * 1024});
    return {ok: true, output: output.trim()};
  } catch (error) {
    return {ok: false, output: `${label} failed: ${String(error.stdout || error.stderr || error.message).trim()}`};
  }
}

function handoff(beadIds, cwd, state, commitSha, validation) {
  const comment = `YAADT handoff: state=${state}; checkpoint=${commitSha || 'none'}; validation=${validation}`;
  for (const beadId of (beadIds || []).filter(id => id && id !== 'manual')) {
    try {
      execFileSync('bd', ['comment', beadId, comment], {cwd, encoding: 'utf8', maxBuffer: 1024 * 1024});
    } catch (error) {
      return {ok: false, error: `Beads handoff failed for ${beadId}: ${String(error.stderr || error.message).trim()}`};
    }
  }
  return {ok: true};
}

function runTool(command, args, cwd) {
  return new Promise((resolve) => {
    const child = spawn(command, args, {cwd, env: process.env, stdio: ['ignore', 'pipe', 'pipe']});
    let stdout = ''; let stderr = '';
    child.stdout.on('data', data => { stdout += data.toString(); });
    child.stderr.on('data', data => { stderr += data.toString(); });
    child.on('error', error => resolve({ok: false, output: error.message}));
    child.on('close', code => resolve({ok: code === 0, output: `${stdout}${stderr}`.trim(), code}));
  });
}

function run(body) {
  activeRuns++;
  return new Promise((resolve) => {
    const configDir = fs.mkdtempSync(path.join(os.tmpdir(), 'yaadt-opencode-'));
    const cwd = body.cwd || '/workspace';
    fs.mkdirSync(cwd, {recursive: true});
    const configPath = path.join(configDir, 'opencode.json');
    const modelRaw = body.modelIdentifier || 'qwen-coder';
    const modelParts = modelRaw.split('/');
    const provider = modelParts.length > 1 ? modelParts[0] : (body.providerIdentifier || 'external');
    const model = modelParts.length > 1 ? modelParts.slice(1).join('/') : modelRaw;
    const config = {
      '$schema': 'https://opencode.ai/config.json',
      autoupdate: false,
      share: 'disabled',
      provider: {[provider]: {npm: '@ai-sdk/openai-compatible', name: provider,
        options: {baseURL: body.baseUrl || undefined, apiKey: '{env:LLM_API_KEY}'},
        models: {[model]: {name: model}}}},
      model: `${provider}/${model}`
    };
    fs.writeFileSync(configPath, JSON.stringify(config));
    console.log(`run started cwd=${cwd} model=${provider}/${model}`);
    const child = spawn(opencodeBin, ['run', '--format', 'json', body.prompt], {
      cwd,
      env: {...process.env, OPENCODE_CONFIG: configPath, LLM_API_KEY: body.apiKey || process.env.LLM_API_KEY || ''},
      stdio: ['ignore', 'pipe', 'pipe']
    });
    const events = [];
    let output = '';
    const started = new Date().toISOString();
    const collect = (type, data) => {
      const text = data.toString().trim();
      if (!text) return;
      output += `${text}\n`;
      events.push({eventType: type, payload: text, redacted: true, createdAt: new Date().toISOString()});
    };
    child.stdout.on('data', data => collect('OPENCODE', data));
    child.stderr.on('data', data => collect('STDERR', data));
    let timedOut = false;
    const timer = setTimeout(() => { timedOut = true; child.kill('SIGTERM'); }, timeoutMs);
    let settled = false;
    const finish = (code, signal, error) => {
      if (settled) return;
      settled = true;
      activeRuns--;
      clearTimeout(timer);
      fs.rmSync(configDir, {recursive: true, force: true});
      events.unshift({eventType: 'RUN_STARTED', payload: started, redacted: true, createdAt: started});
      let commitSha = null;
      if (code === 0) {
        commitSha = checkpoint(cwd);
        events.push({eventType: 'CHECKPOINT', payload: commitSha ? `commit=${commitSha}` : 'no Git checkpoint captured', redacted: true, createdAt: new Date().toISOString()});
      }
      let validationFailed = false;
      const validationResults = [];
      if (code === 0) {
        for (const [label, command] of [['BUILD', body.buildCommands], ['TEST', body.testCommands]]) {
          const result = validateCommand(command, cwd, label);
          validationResults.push(`${label}=${result.ok ? 'PASSED' : 'FAILED'}`);
          events.push({eventType: `VALIDATION_${label}`, payload: result.output || `${label} passed`, redacted: true, createdAt: new Date().toISOString()});
          if (!result.ok) validationFailed = true;
        }
      }
      let state = code === 0 && !validationFailed ? 'SUCCEEDED' : (timedOut ? 'TIMED_OUT' : 'FAILED');
      if (body.beadIds && body.beadIds.length) {
        const result = handoff(body.beadIds, cwd, state, commitSha, validationResults.join(','));
        events.push({eventType: result.ok ? 'BEADS_HANDOFF' : 'RUN_ERROR', payload: result.ok ? `beads=${body.beadIds.join(',')}` : result.error, redacted: true, createdAt: new Date().toISOString()});
        if (!result.ok) { validationFailed = true; state = 'FAILED'; }
      }
      if (error) events.push({eventType: 'RUN_ERROR', payload: error.message, redacted: true, createdAt: new Date().toISOString()});
      if (timedOut) events.push({eventType: 'RUN_ERROR', payload: `runner timeout after ${timeoutMs}ms`, redacted: true, createdAt: new Date().toISOString()});
      events.push({eventType: state === 'SUCCEEDED' ? 'RUN_COMPLETED' : 'RUN_FAILED', payload: `exit=${code} signal=${signal || 'none'}`, redacted: true, createdAt: new Date().toISOString()});
      events.forEach((event, index) => { event.sequenceNumber = index + 1; });
      resolve({state, commitSha,
        errorCode: state === 'SUCCEEDED' ? null : (timedOut ? 'RUN_TIMEOUT' : (error ? 'OPENCODE_UNAVAILABLE' : 'OPENCODE_FAILED')),
        errorMessage: state === 'SUCCEEDED' ? null : (error ? error.message : (output.slice(-4000) || `terminated by ${signal || 'unknown signal'}`)), events});
      console.log(`run finished state=${state} exit=${code} signal=${signal || 'none'}`);
    };
    child.on('error', error => finish(-1, null, error));
    child.on('close', (code, signal) => finish(code, signal, null));
  });
}

const server = http.createServer((req, res) => {
  if (req.method === 'GET' && req.url === '/health') return response(res, 200, {status: 'UP'});
  if (req.method === 'POST' && (req.url === '/admin/beads/reset-ownership' || req.url === '/admin/cocoindex/reindex')) {
    if (!token || req.headers.authorization !== `Bearer ${token}`) return response(res, 401, {error: 'unauthorized'});
    let data = ''; req.on('data', chunk => { data += chunk; });
    return req.on('end', async () => {
      try {
        const body = data ? JSON.parse(data) : {};
        let result;
        if (req.url === '/admin/beads/reset-ownership') {
          const uid = process.env.YAADT_WORKSPACE_UID || '1000';
          const gid = process.env.YAADT_WORKSPACE_GID || '1000';
          result = await runTool('chown', ['-R', `${uid}:${gid}`, '/workspace/.beads'], '/workspace');
        } else {
          const workspacePath = path.resolve(body.workspacePath || '');
          if (workspacePath !== '/workspace' && !workspacePath.startsWith('/workspace/')) return response(res, 400, {error: 'workspacePath must be inside /workspace'});
          result = await runTool('ccc', ['index'], workspacePath);
        }
        return response(res, result.ok ? 200 : 500, {state: result.ok ? 'SUCCEEDED' : 'FAILED', output: result.output || '', exitCode: result.code ?? null});
      } catch (error) { return response(res, 400, {error: error.message}); }
    });
  }
  if (req.method !== 'POST' || req.url !== '/run') return response(res, 404, {error: 'not found'});
  if (!token || req.headers.authorization !== `Bearer ${token}`) return response(res, 401, {error: 'unauthorized'});
  if (activeRuns >= maxConcurrentRuns) return response(res, 429, {
    error: `runner busy: ${activeRuns}/${maxConcurrentRuns} runs active`
  });
  let data = ''; req.on('data', chunk => { data += chunk; if (data.length > 1_000_000) req.destroy(); });
  req.on('end', async () => { try { response(res, 200, await run(JSON.parse(data))); } catch (e) { response(res, 400, {error: e.message}); } });
});
server.listen(port, '0.0.0.0');
