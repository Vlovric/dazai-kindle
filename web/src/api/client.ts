const BASE_URL = 'http://localhost:8080/api';

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
