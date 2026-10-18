# Gboard Rambler — LSPosed API 102

Experimental runtime port based on [PixelBoard](https://github.com/Akshayykadam/PixelBoard) (GPL-3.0, Akshay Kadam).

Source currently provides version-conditional Rambler method mappings, lifecycle hooks, read-only selection tracking and guarded typed flag transforms. **All experiment toggles are off by default.** Nothing has been compiled or validated on an Android device; Rambler and Writing Tools must not be presented as working features.

Important fixes: require a genuine Gboard Context before overriding flags, respect `enable_jetson=false` for *every* override, and make reflected target methods accessible. The code targets Modern libxposed API 102 and the GitHub Actions workflow runs manually only.

Development notes: [Rambler migration](docs/RAMBLER_PORT.md), [validation](docs/VALIDATION.md), [patch plan](docs/PORTING.md).

Copyright of upstream-derived design and flag definitions remains with their contributors. GPL-3.0.
