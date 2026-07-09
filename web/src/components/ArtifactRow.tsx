import type { ArtifactSummary } from "../dto/runs"

export function ArtifactRow({ label, artifact, selected, onToggleSelect, onOpen }: {
    label: string
    artifact: ArtifactSummary
    selected: boolean
    onToggleSelect: () => void
    onOpen: () => void
}) {
    return (
        <div
            className={`flex items-center gap-md border rounded-lg px-lg py-md transition-colors ${selected
                ? "border-primary bg-secondary-container"
                : "border-outline-variant bg-surface-container-lowest hover:border-primary"
                }`}
        >
            <button type="button" onClick={onOpen} className="flex-1 flex items-center justify-between gap-md text-left min-w-0">
                <div className="min-w-0">
                    <p className="font-body text-body-md text-on-surface">{label}</p>
                    <p className="font-body text-label-sm text-secondary truncate">{artifact.name}</p>
                </div>
                <span className="font-body text-label-md text-secondary shrink-0">{artifact.format}</span>
            </button>
            <input
                type="checkbox"
                checked={selected}
                onChange={onToggleSelect}
                className="w-4 h-4 accent-primary shrink-0"
            />
        </div>
    )
}
