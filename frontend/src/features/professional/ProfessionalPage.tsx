import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { ApiError, api } from '../../lib/api'
import { useAuth } from '../auth/auth-context'
import type { ProfessionalProfile, ServiceOffering, Specialty } from './types'
import { GoogleCalendarCard } from './GoogleCalendarCard'

type ProfileForm = { slug: string; bio: string; phone: string; timeZone: string; published: boolean }
type ServiceForm = { name: string; description: string; durationMinutes: number; priceAmount: number; currency: string; active: boolean }

const serviceDefaults: ServiceForm = {
  name: '', description: '', durationMinutes: 60, priceAmount: 0, currency: 'CLP', active: true,
}

function slugify(value: string) {
  return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '')
}

export function ProfessionalPage() {
  const { user, request, logout } = useAuth()
  const [profile, setProfile] = useState<ProfessionalProfile | null>(null)
  const [specialties, setSpecialties] = useState<Specialty[]>([])
  const [selectedSpecialties, setSelectedSpecialties] = useState<string[]>([])
  const [services, setServices] = useState<ServiceOffering[]>([])
  const [editing, setEditing] = useState<ServiceOffering | null>(null)
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const profileForm = useForm<ProfileForm>({ defaultValues: {
    slug: slugify(user?.name ?? ''), bio: '', phone: '',
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone, published: false,
  } })
  const serviceForm = useForm<ServiceForm>({ defaultValues: serviceDefaults })

  useEffect(() => {
    let active = true
    async function load() {
      try {
        const catalog = await api<Specialty[]>('/api/v1/specialties')
        if (active) setSpecialties(catalog)
        try {
          const current = await request<ProfessionalProfile>('/api/v1/professional/profile')
          const offerings = await request<ServiceOffering[]>('/api/v1/professional/services')
          if (!active) return
          setProfile(current); setServices(offerings)
          setSelectedSpecialties(current.specialties.map((item) => item.id))
          profileForm.reset({
            slug: current.slug, bio: current.bio ?? '', phone: current.phone ?? '',
            timeZone: current.timeZone, published: current.published,
          })
        } catch (caught) {
          if (!(caught instanceof ApiError) || caught.status !== 404) throw caught
        }
      } catch (caught) {
        if (active) setError(caught instanceof Error ? caught.message : 'No fue posible cargar el perfil')
      } finally { if (active) setLoading(false) }
    }
    void load()
    return () => { active = false }
  }, [profileForm, request])

  async function saveProfile(values: ProfileForm) {
    setError(''); setNotice('')
    try {
      const saved = await request<ProfessionalProfile>('/api/v1/professional/profile', {
        method: 'PUT', body: JSON.stringify({ ...values, specialtyIds: selectedSpecialties }),
      })
      setProfile(saved); setNotice('Perfil guardado correctamente.')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No fue posible guardar el perfil') }
  }

  async function saveService(values: ServiceForm) {
    setError(''); setNotice('')
    try {
      const path = editing ? `/api/v1/professional/services/${editing.id}` : '/api/v1/professional/services'
      const saved = await request<ServiceOffering>(path, {
        method: editing ? 'PUT' : 'POST', body: JSON.stringify(values),
      })
      setServices((current) => editing
        ? current.map((item) => item.id === saved.id ? saved : item)
        : [saved, ...current])
      setEditing(null); serviceForm.reset(serviceDefaults); setNotice('Servicio guardado correctamente.')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No fue posible guardar el servicio') }
  }

  function editService(service: ServiceOffering) {
    setEditing(service)
    serviceForm.reset({ ...service, description: service.description ?? '' })
    document.getElementById('service-form')?.scrollIntoView({ behavior: 'smooth' })
  }

  async function deactivateService(id: string) {
    setError(''); setNotice('')
    try {
      await request<void>(`/api/v1/professional/services/${id}`, { method: 'DELETE' })
      setServices((current) => current.map((item) => item.id === id ? { ...item, active: false } : item))
      if (profile?.published && services.filter((item) => item.active && item.id !== id).length === 0) {
        setProfile({ ...profile, published: false }); profileForm.setValue('published', false)
      }
      setNotice('Servicio desactivado.')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No fue posible desactivar el servicio') }
  }

  if (loading) return <PageLoading />

  return <main className="container py-4 py-lg-5">
    <nav className="d-flex justify-content-between align-items-center mb-5">
      <Link className="brand text-decoration-none" to="/dashboard">Reservas</Link>
      <button className="btn btn-outline-secondary btn-sm" onClick={() => void logout()}>Cerrar sesión</button>
    </nav>
    <div className="row g-5">
      <aside className="col-lg-4">
        <p className="eyebrow">ESPACIO PROFESIONAL</p>
        <h1 className="profile-title">Tu vitrina y tus servicios.</h1>
        <p className="text-secondary">Configura lo que verán tus clientes antes de construir la agenda.</p>
        <div className="d-flex flex-column align-items-start gap-2">{profile && <Link to="/configurar-agenda">Configurar horarios →</Link>}
          {profile?.published && <Link to={`/p/${profile.slug}`} target="_blank" rel="noreferrer">Ver perfil público →</Link>}</div>
      </aside>
      <div className="col-lg-8">
        {notice && <div className="alert alert-success" role="status">{notice}</div>}
        {error && <div className="alert alert-danger" role="alert">{error}</div>}
        <section className="workspace-card mb-4">
          <h2>Perfil profesional</h2>
          <form onSubmit={profileForm.handleSubmit(saveProfile)}>
            <div className="mb-3"><label className="form-label" htmlFor="slug">URL pública</label>
              <div className="input-group"><span className="input-group-text">/p/</span><input id="slug" className="form-control" {...profileForm.register('slug', { required: true, pattern: /^[a-z0-9]+(?:-[a-z0-9]+)*$/ })} /></div></div>
            <div className="mb-3"><label className="form-label" htmlFor="bio">Presentación</label>
              <textarea id="bio" rows={4} className="form-control" maxLength={1000} {...profileForm.register('bio')} /></div>
            <div className="row"><div className="col-md-6 mb-3"><label className="form-label" htmlFor="phone">Teléfono</label>
              <input id="phone" className="form-control" {...profileForm.register('phone')} /></div>
              <div className="col-md-6 mb-3"><label className="form-label" htmlFor="timeZone">Zona horaria</label>
                <input id="timeZone" className="form-control" {...profileForm.register('timeZone', { required: true })} /></div></div>
            <fieldset className="mb-4"><legend className="form-label">Especialidades</legend>
              <div className="row g-2">{specialties.map((item) => <div className="col-md-6" key={item.id}><label className="specialty-option">
                <input type="checkbox" checked={selectedSpecialties.includes(item.id)} onChange={(event) => setSelectedSpecialties((current) => event.target.checked ? [...current, item.id] : current.filter((id) => id !== item.id))} /> {item.name}
              </label></div>)}</div></fieldset>
            <div className="form-check form-switch mb-4"><input id="published" type="checkbox" className="form-check-input" disabled={services.every((item) => !item.active)} {...profileForm.register('published')} />
              <label className="form-check-label" htmlFor="published">Publicar perfil</label>
              {services.every((item) => !item.active) && <div className="form-text">Necesitas al menos un servicio activo.</div>}</div>
            <button className="btn btn-primary" disabled={profileForm.formState.isSubmitting}>{profileForm.formState.isSubmitting ? 'Guardando…' : 'Guardar perfil'}</button>
          </form>
        </section>

        {profile && <GoogleCalendarCard request={request} />}

        {profile && <section className="workspace-card" id="service-form">
          <h2>{editing ? 'Editar servicio' : 'Nuevo servicio'}</h2>
          <form onSubmit={serviceForm.handleSubmit(saveService)}>
            <div className="mb-3"><label className="form-label" htmlFor="service-name">Nombre</label><input id="service-name" className="form-control" {...serviceForm.register('name', { required: true })} /></div>
            <div className="mb-3"><label className="form-label" htmlFor="description">Descripción</label><textarea id="description" className="form-control" {...serviceForm.register('description')} /></div>
            <div className="row"><div className="col-md-4 mb-3"><label className="form-label" htmlFor="duration">Duración (min)</label><input id="duration" type="number" min="10" max="720" className="form-control" {...serviceForm.register('durationMinutes', { valueAsNumber: true, required: true })} /></div>
              <div className="col-md-4 mb-3"><label className="form-label" htmlFor="price">Precio</label><input id="price" type="number" min="0" step="0.01" className="form-control" {...serviceForm.register('priceAmount', { valueAsNumber: true, required: true })} /></div>
              <div className="col-md-4 mb-3"><label className="form-label" htmlFor="currency">Moneda</label><input id="currency" maxLength={3} className="form-control text-uppercase" {...serviceForm.register('currency', { required: true, pattern: /^[A-Z]{3}$/ })} /></div></div>
            {editing && <div className="form-check mb-3"><input id="active" type="checkbox" className="form-check-input" {...serviceForm.register('active')} /><label htmlFor="active" className="form-check-label">Servicio activo</label></div>}
            <div className="d-flex gap-2"><button className="btn btn-primary" disabled={serviceForm.formState.isSubmitting}>{editing ? 'Actualizar' : 'Agregar servicio'}</button>
              {editing && <button type="button" className="btn btn-light" onClick={() => { setEditing(null); serviceForm.reset(serviceDefaults) }}>Cancelar</button>}</div>
          </form>
          <div className="service-list mt-5">{services.map((service) => <article className="service-row" key={service.id}>
            <div><div className="d-flex gap-2 align-items-center"><strong>{service.name}</strong><span className={`badge ${service.active ? 'text-bg-success' : 'text-bg-secondary'}`}>{service.active ? 'Activo' : 'Inactivo'}</span></div>
              <small className="text-secondary">{service.durationMinutes} min · {new Intl.NumberFormat('es-CL', { style: 'currency', currency: service.currency }).format(service.priceAmount)}</small></div>
            <div className="d-flex gap-2"><button className="btn btn-sm btn-outline-secondary" onClick={() => editService(service)}>Editar</button>{service.active && <button className="btn btn-sm btn-outline-danger" onClick={() => void deactivateService(service.id)}>Desactivar</button>}</div>
          </article>)}</div>
        </section>}
      </div>
    </div>
  </main>
}

function PageLoading() {
  return <main className="min-vh-100 d-flex align-items-center justify-content-center"><div className="spinner-border text-success"><span className="visually-hidden">Cargando</span></div></main>
}
