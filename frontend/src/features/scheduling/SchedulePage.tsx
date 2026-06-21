import { useEffect, useState } from 'react'
import { useForm, useWatch, type UseFormRegister } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/auth-context'
import type { BookingPolicy, DayOfWeek, ScheduleException, SchedulePeriod } from './types'

const days: { value: DayOfWeek; label: string }[] = [
  { value: 'MONDAY', label: 'Lunes' }, { value: 'TUESDAY', label: 'Martes' },
  { value: 'WEDNESDAY', label: 'Miércoles' }, { value: 'THURSDAY', label: 'Jueves' },
  { value: 'FRIDAY', label: 'Viernes' }, { value: 'SATURDAY', label: 'Sábado' },
  { value: 'SUNDAY', label: 'Domingo' },
]

type ExceptionForm = {
  date: string
  type: 'BLOCKED' | 'AVAILABLE'
  fullDay: boolean
  startTime: string
  endTime: string
  reason: string
}

function isoDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function SchedulePage() {
  const { request, logout } = useAuth()
  const [periods, setPeriods] = useState<SchedulePeriod[]>([])
  const [exceptions, setExceptions] = useState<ScheduleException[]>([])
  const [loading, setLoading] = useState(true)
  const [savingSchedule, setSavingSchedule] = useState(false)
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')
  const policyForm = useForm<BookingPolicy>()
  const exceptionForm = useForm<ExceptionForm>({ defaultValues: {
    date: isoDate(new Date()), type: 'BLOCKED', fullDay: true,
    startTime: '09:00', endTime: '18:00', reason: '',
  } })
  const exceptionType = useWatch({ control: exceptionForm.control, name: 'type' })
  const fullDay = useWatch({ control: exceptionForm.control, name: 'fullDay' })

  useEffect(() => {
    let active = true
    const from = isoDate(new Date())
    const toDate = new Date(); toDate.setDate(toDate.getDate() + 180)
    Promise.all([
      request<SchedulePeriod[]>('/api/v1/professional/schedule/weekly'),
      request<BookingPolicy>('/api/v1/professional/schedule/policy'),
      request<ScheduleException[]>(`/api/v1/professional/schedule/exceptions?from=${from}&to=${isoDate(toDate)}`),
    ]).then(([weekly, policy, exceptionList]) => {
      if (!active) return
      setPeriods(weekly); setExceptions(exceptionList); policyForm.reset(policy)
    }).catch((caught) => { if (active) setError(caught instanceof Error ? caught.message : 'No fue posible cargar la agenda') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [policyForm, request])

  function addPeriod(dayOfWeek: DayOfWeek) {
    setPeriods((current) => [...current, { dayOfWeek, startTime: '09:00', endTime: '18:00' }])
  }

  function updatePeriod(index: number, field: 'startTime' | 'endTime', value: string) {
    setPeriods((current) => current.map((period, itemIndex) => itemIndex === index ? { ...period, [field]: value } : period))
  }

  async function saveSchedule() {
    setSavingSchedule(true); setError(''); setNotice('')
    try {
      const saved = await request<SchedulePeriod[]>('/api/v1/professional/schedule/weekly', {
        method: 'PUT', body: JSON.stringify({ periods: periods.map(({ dayOfWeek, startTime, endTime }) => ({ dayOfWeek, startTime, endTime })) }),
      })
      setPeriods(saved); setNotice('Horario semanal guardado.')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No fue posible guardar el horario') }
    finally { setSavingSchedule(false) }
  }

  async function savePolicy(values: BookingPolicy) {
    setError(''); setNotice('')
    try {
      const saved = await request<BookingPolicy>('/api/v1/professional/schedule/policy', {
        method: 'PUT', body: JSON.stringify(values),
      })
      policyForm.reset(saved); setNotice('Política de reservas guardada.')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No fue posible guardar la política') }
  }

  async function addException(values: ExceptionForm) {
    setError(''); setNotice('')
    try {
      const usesHours = values.type === 'AVAILABLE' || !values.fullDay
      const saved = await request<ScheduleException>('/api/v1/professional/schedule/exceptions', {
        method: 'POST', body: JSON.stringify({
          date: values.date, type: values.type,
          startTime: usesHours ? values.startTime : null,
          endTime: usesHours ? values.endTime : null,
          reason: values.reason,
        }),
      })
      setExceptions((current) => [...current, saved].sort((a, b) => a.date.localeCompare(b.date)))
      exceptionForm.reset({ ...values, reason: '' }); setNotice('Excepción agregada.')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No fue posible agregar la excepción') }
  }

  async function removeException(id: string) {
    try {
      await request<void>(`/api/v1/professional/schedule/exceptions/${id}`, { method: 'DELETE' })
      setExceptions((current) => current.filter((item) => item.id !== id)); setNotice('Excepción eliminada.')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No fue posible eliminar la excepción') }
  }

  if (loading) return <main className="min-vh-100 d-flex align-items-center justify-content-center"><div className="spinner-border text-success"><span className="visually-hidden">Cargando</span></div></main>

  return <main className="container py-4 py-lg-5">
    <nav className="d-flex justify-content-between align-items-center mb-5"><Link className="brand text-decoration-none" to="/dashboard">Reservas</Link><button className="btn btn-outline-secondary btn-sm" onClick={() => void logout()}>Cerrar sesión</button></nav>
    <div className="row g-5"><aside className="col-lg-4"><p className="eyebrow">DISPONIBILIDAD</p><h1 className="profile-title">Diseña una semana respirable.</h1><p className="text-secondary">Tu horario vive en tu zona horaria. Los bloqueos y días especiales siempre tienen prioridad.</p><Link to="/perfil-profesional">← Volver al perfil</Link></aside>
      <div className="col-lg-8">{notice && <div className="alert alert-success">{notice}</div>}{error && <div className="alert alert-danger">{error}</div>}
        <section className="workspace-card mb-4"><h2>Semana habitual</h2>
          {days.map((day) => <div className="schedule-day" key={day.value}><div className="schedule-day-name"><strong>{day.label}</strong><button className="btn btn-sm btn-link" onClick={() => addPeriod(day.value)}>+ tramo</button></div>
            <div className="schedule-periods">{periods.map((period, index) => period.dayOfWeek === day.value && <div className="d-flex align-items-center gap-2" key={`${day.value}-${index}`}>
              <input aria-label={`Inicio ${day.label}`} type="time" className="form-control" value={period.startTime.slice(0, 5)} onChange={(event) => updatePeriod(index, 'startTime', event.target.value)} />
              <span>—</span><input aria-label={`Fin ${day.label}`} type="time" className="form-control" value={period.endTime.slice(0, 5)} onChange={(event) => updatePeriod(index, 'endTime', event.target.value)} />
              <button className="btn btn-sm btn-outline-danger" aria-label={`Eliminar tramo ${day.label}`} onClick={() => setPeriods((current) => current.filter((_, itemIndex) => itemIndex !== index))}>×</button>
            </div>)}{periods.every((period) => period.dayOfWeek !== day.value) && <span className="text-secondary small">No disponible</span>}</div></div>)}
          <button className="btn btn-primary mt-4" onClick={() => void saveSchedule()} disabled={savingSchedule}>{savingSchedule ? 'Guardando…' : 'Guardar semana'}</button>
        </section>

        <section className="workspace-card mb-4"><h2>Reglas de reserva</h2><form onSubmit={policyForm.handleSubmit(savePolicy)}><div className="row">
          <NumberField label="Anticipación mínima (min)" name="minimumNoticeMinutes" register={policyForm.register} min={0} max={43200} />
          <NumberField label="Ventana máxima (días)" name="bookingWindowDays" register={policyForm.register} min={1} max={365} />
          <NumberField label="Intervalo entre inicios (min)" name="slotIntervalMinutes" register={policyForm.register} min={5} max={120} />
          <NumberField label="Buffer posterior (min)" name="bufferAfterMinutes" register={policyForm.register} min={0} max={180} />
        </div><button className="btn btn-primary" disabled={policyForm.formState.isSubmitting}>Guardar reglas</button></form></section>

        <section className="workspace-card"><h2>Días especiales y bloqueos</h2><form onSubmit={exceptionForm.handleSubmit(addException)}><div className="row">
          <div className="col-md-6 mb-3"><label className="form-label" htmlFor="exception-date">Fecha</label><input id="exception-date" type="date" min={isoDate(new Date())} className="form-control" {...exceptionForm.register('date', { required: true })} /></div>
          <div className="col-md-6 mb-3"><label className="form-label" htmlFor="exception-type">Tipo</label><select id="exception-type" className="form-select" {...exceptionForm.register('type')}><option value="BLOCKED">Bloqueo</option><option value="AVAILABLE">Horario especial</option></select></div>
        </div>{exceptionType === 'BLOCKED' && <div className="form-check mb-3"><input id="full-day" type="checkbox" className="form-check-input" {...exceptionForm.register('fullDay')} /><label htmlFor="full-day" className="form-check-label">Bloquear todo el día</label></div>}
          {(exceptionType === 'AVAILABLE' || !fullDay) && <div className="row"><div className="col-6 mb-3"><label className="form-label" htmlFor="exception-start">Desde</label><input id="exception-start" type="time" className="form-control" {...exceptionForm.register('startTime')} /></div><div className="col-6 mb-3"><label className="form-label" htmlFor="exception-end">Hasta</label><input id="exception-end" type="time" className="form-control" {...exceptionForm.register('endTime')} /></div></div>}
          <div className="mb-3"><label className="form-label" htmlFor="reason">Motivo interno</label><input id="reason" className="form-control" maxLength={200} {...exceptionForm.register('reason')} /></div><button className="btn btn-primary">Agregar excepción</button></form>
          <div className="mt-5">{exceptions.map((item) => <article className="service-row" key={item.id}><div><strong>{new Intl.DateTimeFormat('es-CL', { dateStyle: 'long', timeZone: 'UTC' }).format(new Date(`${item.date}T12:00:00Z`))}</strong><div className="small text-secondary">{item.type === 'AVAILABLE' ? 'Horario especial' : item.startTime ? 'Bloqueo parcial' : 'Día bloqueado'}{item.startTime && ` · ${item.startTime.slice(0, 5)}–${item.endTime?.slice(0, 5)}`}{item.reason && ` · ${item.reason}`}</div></div><button className="btn btn-sm btn-outline-danger" onClick={() => void removeException(item.id)}>Eliminar</button></article>)}</div>
        </section>
      </div></div>
  </main>
}

type PolicyField = keyof BookingPolicy
function NumberField({ label, name, register, min, max }: { label: string; name: PolicyField; register: UseFormRegister<BookingPolicy>; min: number; max: number }) {
  return <div className="col-md-6 mb-3"><label className="form-label" htmlFor={name}>{label}</label><input id={name} type="number" min={min} max={max} className="form-control" {...register(name, { valueAsNumber: true, required: true })} /></div>
}
