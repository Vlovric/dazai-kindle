// Relative on purpose: in production the built frontend is served by the same
// Spring Boot jar that exposes the API, so same-origin "/api" always resolves
// correctly regardless of host/port. In dev, Vite's proxy (see vite.config.ts)
// forwards "/api" to the backend running on localhost:8080.
const BASE_URL = '/api';

async function throwApiError(response: Response, fallback: string): Promise<never> {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? fallback);
}

export async function apiGet<T>(path: string): Promise<T>{
    const response = await fetch(`${BASE_URL}${path}`);
    if(!response.ok){
        await throwApiError(response, `GET ${path} failed: ${response.status} ${response.statusText}`);
    }
    return response.json()
}

export async function apiPut<T>(path: string, body: unknown): Promise<T>{
    const response = await fetch(`${BASE_URL}${path}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });
    if(!response.ok){
        await throwApiError(response, `PUT ${path} failed: ${response.status} ${response.statusText}`);
    }
    return response.json()
}

export async function apiPost<T>(path: string, body: unknown): Promise<T>{
    const response = await fetch(`${BASE_URL}${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });
    if(!response.ok){
        await throwApiError(response, `POST ${path} failed: ${response.status} ${response.statusText}`);
    }
    return response.json()
}

export async function apiPostEmpty(path: string): Promise<void>{
    const response = await fetch(`${BASE_URL}${path}`, { method: 'POST' });
    if(!response.ok){
        await throwApiError(response, `POST ${path} failed: ${response.status} ${response.statusText}`);
    }
}

export async function apiDelete(path: string): Promise<void>{
    const response = await fetch(`${BASE_URL}${path}`, { method: 'DELETE' });
    if(!response.ok){
        await throwApiError(response, `DELETE ${path} failed: ${response.status} ${response.statusText}`);
    }
}

function filenameFromContentDisposition(header: string | null, fallback: string): string {
    const match = header?.match(/filename="?([^"]+)"?/)
    return match ? match[1] : fallback
}

export async function apiGetBlob(path: string, fallbackFilename: string): Promise<{ blob: Blob, filename: string }>{
    const response = await fetch(`${BASE_URL}${path}`);
    if(!response.ok){
        await throwApiError(response, `GET ${path} failed: ${response.status} ${response.statusText}`);
    }
    const blob = await response.blob()
    const filename = filenameFromContentDisposition(response.headers.get('Content-Disposition'), fallbackFilename)
    return { blob, filename }
}

export async function apiPostBlob(path: string, body: unknown, fallbackFilename: string): Promise<{ blob: Blob, filename: string }>{
    const response = await fetch(`${BASE_URL}${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    })
    if(!response.ok){
        await throwApiError(response, `POST ${path} failed: ${response.status} ${response.statusText}`)
    }
    const blob = await response.blob()
    const filename = filenameFromContentDisposition(response.headers.get('Content-Disposition'), fallbackFilename)
    return { blob, filename }
}

export async function apiPostForm<T>(path: string, form: FormData): Promise<T>{
    const response = await fetch(`${BASE_URL}${path}`, {
        method: 'POST',
        body: form,
    });
    if(!response.ok){
        await throwApiError(response, `POST ${path} failed: ${response.status} ${response.statusText}`);
    }
    return response.json()
}
