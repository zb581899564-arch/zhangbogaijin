-- Portable report provenance query.
-- Source values are transcribed from the verified run records listed in
-- REPORT_SOURCE_NOTES.md; this query describes the normalized report view.
SELECT metric, full_value, baseline_value
FROM p86_100k_engineering_comparison;
