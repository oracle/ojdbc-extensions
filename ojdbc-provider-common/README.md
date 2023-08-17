# Oracle JDBC Providers Common Module
This module contains common code that is shared by all providers in this 
project. The API of this module is mainly designed only for other modules
in this project to consume. The only exceptions to this are described in the
remaining sections of this document. All other APIs of this module are not
stable, and may become inaccessible in a later release.

## Clearing Cached Resources 
Most providers in this project will cache the resources they request from a
cloud service. Cached resources can be cleared by calling the
`clearAllCaches()` method of the  `oracle.jdbc.provider.cache.CacheController`
contained in this module.
