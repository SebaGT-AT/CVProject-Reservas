export type ApiProblem = {
  title?: string
  detail?: string
  errors?: Record<string, string>
}

const API_URL = import.meta.env.VITE_API_URL ?? ''

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })

  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as ApiProblem
    throw new Error(problem.detail ?? 'No fue posible completar la solicitud')
  }

  return response.json() as Promise<T>
}

