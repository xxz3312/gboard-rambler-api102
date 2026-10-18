# PixelBoard LSPosed — API 102

Experimental Modern LSPosed port for official Gboard, adapted from [PixelBoard](https://github.com/Akshayykadam/PixelBoard) (GPL-3.0).

Source includes version-scoped Rambler lifecycle interception, selection read observation, typed stock flag policy and fail-closed getter mappings. **The getter hook currently passes through unmodified values**; Rambler activation and AI Writing Tools are not implemented. Hooks are disabled by default.

See [Rambler porting notes](docs/RAMBLER_PORT.md) and [overview](docs/PORTING.md).

No automated builds on push: GitHub Actions can only be run manually via `workflow_dispatch`. This source has not been compiled or tested.

Licensed GPL-3.0 with attribution to Akshay Kadam's PixelBoard.
