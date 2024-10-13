-- WEB BACKEND CONFIGURATION
-- (set one time at DB creation)
--
-- Set the journal mode to "WAL" for better throughput. Adjust
-- busy timeout, synchronous setting, cache size, temp store, and
-- foreign keys for optimal use based on hardware and workload.
--
-- CPU RESOURCES
--
-- Enable Write-Ahead Logging for better throughput. WAL allows
-- Non-blocking readers concurrently with single-threaded writer.
PRAGMA journal_mode=WAL;
-- Disk sync method. NORMAL is okay for web DB.
PRAGMA synchronous=NORMAL;
-- Wait these many milliseconds for locked DB to become available.
PRAGMA busy_timeout=5000;
-- Use up to N threads on multi-core systems.
PRAGMA threads=4;
-- MEMORY RESOURCES
-- Store temporary tables in memory for more performance.
PRAGMA temp_store=MEMORY;
-- Page Size in bytes (4KB is default, make it explicit anyway)
PRAGMA page_size=4096;
-- Pages of cache. Negative number is absolute value in KiB.
-- Positive number is number of pages. Bump high, to say 1GB,
-- to reduce disk access for large datasets.
PRAGMA cache_size=-50000; -- 50 MiB
--
-- DATA MANAGEMENT
--
-- Enforce foreign key constraints
PRAGMA foreign_keys=ON;
-- Auto-run index rebuilds and update stats.
PRAGMA optimize;
-- Auto-vacuum incrementally
PRAGMA auto_vacuum=INCREMENTAL;
--
-- OPTIONAL PRAGMAS for more sensitive performance tweaks
--
-- Tune WAL write frequency to main DB.
-- PRAGMA wal_autocheckpoint=1000; -- num pages to wait for checkpoint

-- Control MMAP. Jury is still out whether this improves performance,
-- and if it does, what is the trade-off?
-- PRAGMA map_size=1000000000; -- 1GB mmaped I/O for faster DB access
