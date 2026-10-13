# Controller SSH directory

Place the SSH credentials used for repository access in this directory before
starting Compose:

- `id_ed25519`: private key, mode `0600`
- `id_ed25519.pub`: matching public key, registered with the Git provider
- `known_hosts`: pinned host keys for the Git provider, mode `0644`

The directory is mounted read-only at `/run/yaadt/ssh` in the controller API,
controller worker, and devbox containers. It is intentionally gitignored.

The controller's repository clone executor is not implemented yet. When it is
added, it will use `GIT_SSH_COMMAND` from Compose and this directory without
putting private keys into project or repository records.
