export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export type SchedulePeriod = {
  id?: string
  dayOfWeek: DayOfWeek
  startTime: string
  endTime: string
}

export type BookingPolicy = {
  minimumNoticeMinutes: number
  bookingWindowDays: number
  slotIntervalMinutes: number
  bufferAfterMinutes: number
}

export type ScheduleException = {
  id: string
  date: string
  type: 'BLOCKED' | 'AVAILABLE'
  startTime: string | null
  endTime: string | null
  reason: string | null
}

export type AvailabilitySlot = {
  startAt: string
  endAt: string
  localStartTime: string
  localEndTime: string
}

export type AvailabilityDay = { date: string; slots: AvailabilitySlot[] }
