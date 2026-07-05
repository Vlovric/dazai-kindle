const BASE_URL = 'http://localhost:8080/api';

export async function apiGet<T>(path: string): Promise<T>{
    const response = await fetch(`${BASE_URL}${path}`);
    if(!response.ok){
        throw new Error(`GET ${path} failed: ${response.status} ${response.statusText}`);
    }
    return response.json()
}