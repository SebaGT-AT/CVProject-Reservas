CREATE TABLE booking_policies (
    id UUID PRIMARY KEY,
    professional_id UUID NOT NULL UNIQUE REFERENCES professional_profiles(id) ON DELETE CASCADE,
    minimum_notice_minutes INTEGER NOT NULL DEFAULT 120 CHECK (minimum_notice_minutes BETWEEN 0 AND 43200),
    booking_window_days INTEGER NOT NULL DEFAULT 60 CHECK (booking_window_days BETWEEN 1 AND 365),
    slot_interval_minutes INTEGER NOT NULL DEFAULT 15 CHECK (slot_interval_minutes BETWEEN 5 AND 120),
    buffer_after_minutes INTEGER NOT NULL DEFAULT 0 CHECK (buffer_after_minutes BETWEEN 0 AND 180),
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE weekly_schedule_periods (
    id UUID PRIMARY KEY,
    professional_id UUID NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CHECK (start_time < end_time),
    UNIQUE (professional_id, day_of_week, start_time, end_time)
);

CREATE INDEX ix_weekly_schedule_professional_day
    ON weekly_schedule_periods (professional_id, day_of_week);

CREATE TABLE schedule_exceptions (
    id UUID PRIMARY KEY,
    professional_id UUID NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    exception_date DATE NOT NULL,
    exception_type VARCHAR(20) NOT NULL CHECK (exception_type IN ('BLOCKED', 'AVAILABLE')),
    start_time TIME,
    end_time TIME,
    reason VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL,
    CHECK ((start_time IS NULL AND end_time IS NULL) OR
           (start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)),
    CHECK (exception_type = 'BLOCKED' OR (start_time IS NOT NULL AND end_time IS NOT NULL))
);

CREATE INDEX ix_schedule_exceptions_professional_date
    ON schedule_exceptions (professional_id, exception_date);
