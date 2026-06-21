import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../lib/api'
import type { AvailabilityDay } from '../scheduling/types'
import type { PublicProfessional } from './types'

function dateInZone(timeZone: string) {
  const parts = new Intl.DateTimeFormat('en', {
    timeZone, year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date())
  const value = (type: string) => parts.find((part) => part.type === type)?.value ?? ''
  return `${value('year')}-${value('month')}-${value('day')}`
}

function plusDays(isoDate: string, days: number) {
  const date = new Date(`${isoDate}T12:00:00Z`)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}

export function PublicProfessionalPage() {
  const { slug = '' } = useParams()
  const [professional, setProfessional] = useState<PublicProfessional | null>(null)
  const [selectedService, setSelectedService] = useState('')
  const [availability, setAvailability] = useState<AvailabilityDay[]>([])
  const [loadingSlots, setLoadingSlots] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api<PublicProfessional>(`/api/v1/professionals/${encodeURIComponent(slug)}`)
      .then((data) => { setProfessional(data); setLoadingSlots(true); setSelectedService(data.services[0]?.id ?? '') })
      .catch((caught) => setError(caught instanceof Error ? caught.message : 'Perfil no disponible'))
  }, [slug])

  useEffect(() => {
    if (!professional || !selectedService) return
    const from = dateInZone(professional.timeZone)
    api<AvailabilityDay[]>(`/api/v1/professionals/${encodeURIComponent(slug)}/availability?serviceId=${selectedService}&from=${from}&to=${plusDays(from, 6)}`)
      .then(setAvailability).catch((caught) => setError(caught instanceof Error ? caught.message : 'No fue posible consultar horarios'))
      .finally(() => setLoadingSlots(false))
  }, [professional, selectedService, slug])

  if (error && !professional) return <main className="container py-5"><Link className="brand text-decoration-none" to="/">Reservas</Link><div className="alert alert-warning mt-5">{error}</div></main>
  if (!professional) return <main className="min-vh-100 d-flex align-items-center justify-content-center"><div className="spinner-border text-success"><span className="visually-hidden">Cargando</span></div></main>

  return <main>
    <header className="public-profile-hero"><div className="container py-5">
      <Link className="brand text-decoration-none text-white" to="/">Reservas</Link>
      <div className="public-profile-heading"><div className="d-flex flex-wrap gap-2 mb-3">{professional.specialties.map((item) => <span className="public-tag" key={item.id}>{item.name}</span>)}</div>
        <h1>{professional.name}</h1><p>{professional.bio || 'Profesional independiente disponible para reservas.'}</p></div>
    </div></header>
    <section className="container py-5"><div className="row g-5"><div className="col-lg-7"><p className="eyebrow">SERVICIOS</p>
      {professional.services.map((service) => <button type="button" className={`public-service selectable-service ${selectedService === service.id ? 'selected' : ''}`} key={service.id} onClick={() => { setLoadingSlots(true); setError(''); setSelectedService(service.id) }}>
        <div><h2>{service.name}</h2><p>{service.description}</p><small>{service.durationMinutes} minutos</small></div>
        <strong>{new Intl.NumberFormat('es-CL', { style: 'currency', currency: service.currency }).format(service.priceAmount)}</strong></button>)}</div>
      <aside className="col-lg-5"><div className="availability-panel"><p className="eyebrow">PRÓXIMOS HORARIOS</p>
        {error && <div className="alert alert-warning">{error}</div>}{loadingSlots ? <div className="spinner-border spinner-border-sm text-success"><span className="visually-hidden">Cargando</span></div>
          : availability.some((day) => day.slots.length > 0) ? availability.filter((day) => day.slots.length > 0).map((day) => <div className="availability-day" key={day.date}>
            <strong>{new Intl.DateTimeFormat('es-CL', { weekday: 'long', day: 'numeric', month: 'short', timeZone: 'UTC' }).format(new Date(`${day.date}T12:00:00Z`))}</strong>
            <div className="d-flex flex-wrap gap-2 mt-2">{day.slots.slice(0, 8).map((slot) => <span className="slot-chip" key={slot.startAt}>{slot.localStartTime.slice(0, 5)}</span>)}</div>
          </div>) : <p className="text-secondary">No hay horarios publicados para los próximos siete días.</p>}
        <small className="text-secondary">La reserva en línea se habilitará en la siguiente etapa.</small></div></aside></div></section>
  </main>
}
