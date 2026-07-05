import { useEffect, useState } from "react"
import { getPaths } from "../api/paths"
import type { FolderPath } from "../dto/paths"

export function usePaths() {
    const [data, setData] = useState<FolderPath[] | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        getPaths()
            .then(setData)
            .catch((e) => setError(e.message))
            .finally(() => setLoading(false))
    }, [])

    return { data, loading, error, setData }
}
