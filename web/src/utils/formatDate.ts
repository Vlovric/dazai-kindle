// European/Croatian format: day/month/year, 24h clock, no leading zeros on
// the date (e.g. 8/7/2026 10:37) - independent of the browser's own locale.
export function formatDateTime(iso: string): string {
    const date = new Date(iso)
    const day = date.getDate()
    const month = date.getMonth() + 1
    const year = date.getFullYear()
    const hours = String(date.getHours()).padStart(2, "0")
    const minutes = String(date.getMinutes()).padStart(2, "0")
    return `${day}/${month}/${year} ${hours}:${minutes}`
}
