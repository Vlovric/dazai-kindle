import { useState } from "react"
import { usePaths } from "../hooks/usePaths"
import { PathRow } from "../components/PathRow"
import { updatePath } from "../api/paths"
import { Toast } from "../components/Toast"

const LABELS: Record<string, string> = {
    library: "Library",
    templates: "Templates",
}

export function Paths() {
    const { data: paths, loading, error, setData } = usePaths()
    const [toastMessage, setToastMessage] = useState<string | null>(null)

    async function handleChange(name: string, newPath: string) {
        const updated = await updatePath(name, newPath)
        setData((prev) => prev?.map((p) => (p.name === name ? updated : p)) ?? null)
        setToastMessage("Successfully updated path")
    }

    return (
        <main className="max-w-[1280px] mx-auto px-lg py-xl">
            <h2 className="font-headline text-headline-md text-primary mb-lg">Locations On Filesystem</h2>
            {loading && <p className="font-body text-body-md text-secondary">Loading…</p>}
            {error && <p className="font-body text-body-md text-error">Failed to load paths: {error}</p>}
            {paths && paths.map((p) => (
                <PathRow
                    key={p.name}
                    label={LABELS[p.name] ?? p.name}
                    path={p.path}
                    onChange={(newPath) => handleChange(p.name, newPath)}
                />
            ))}
            {toastMessage && (
                <Toast message={toastMessage} onDismiss={() => setToastMessage(null)} />
            )}
        </main>
    )
}
