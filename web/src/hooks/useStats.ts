import { useEffect, useState } from "react"
import { getStats } from "../api/stats"
import type { StatsResponse } from "../dto/stats"

export function useStats() {
    const [data, setData] = useState<StatsResponse | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        getStats()
            .then(setData)
            .catch((e) => setError(e.message))
            .finally(() => setLoading(false))
    }, [])

    return { data, loading, error }
}