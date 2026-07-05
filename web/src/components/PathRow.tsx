import { useState } from "react"

export function PathRow({ label, path, onChange }: {
    label: string
    path: string
    onChange: (newPath: string) => Promise<void>
}) {
    const [value, setValue] = useState(path)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)

    async function handleChange() {
        setSaving(true)
        setError(null)
        try {
            await onChange(value)
        } catch (e) {
            setError(e instanceof Error ? e.message : "Failed to update path")
        } finally {
            setSaving(false)
        }
    }

    return (
        <div className="bg-surface-container-lowest border border-outline-variant rounded-lg p-lg mb-lg">
            <h3 className="font-headline text-headline-md text-primary mb-sm">{label}</h3>
            <div className="flex items-center gap-md">
                <input
                    type="text"
                    value={value}
                    onChange={(e) => setValue(e.target.value)}
                    className="flex-1 bg-surface border border-outline-variant rounded-lg px-md py-sm text-body-md font-body text-on-surface focus:outline-none focus:border-primary"
                />
                <button
                    onClick={handleChange}
                    disabled={saving}
                    className="bg-surface-container-lowest border border-outline-variant rounded-lg px-md py-sm text-label-md font-body text-primary hover:border-primary transition-colors disabled:opacity-50"
                >
                    {saving ? "Saving…" : "Change"}
                </button>
            </div>
            {error && <p className="text-label-md font-body text-error mt-sm">{error}</p>}
        </div>
    )
}
