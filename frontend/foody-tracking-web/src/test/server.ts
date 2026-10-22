import { setupServer } from 'msw/node'

export const server = setupServer()

export const apiUrl = (path: string) => `${import.meta.env.VITE_API_URL}${path}`
