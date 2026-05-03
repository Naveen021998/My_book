-- NOTE:
-- In the microservices design, the `users` table belongs to `user-service` only.
-- `core-application-service` (ledger/khatabook) uses the JWT `sub` as owner_user_id
-- and must not create or manage the shared `users` table (especially in a single
-- schema setup like `tranning`).
--
-- This migration is intentionally a no-op marker to keep core-service migration
-- versions stable while avoiding cross-service schema conflicts.
SELECT 1;
