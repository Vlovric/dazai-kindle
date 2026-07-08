import type { RunSummary } from "../dto/runs"
import { formatDateTime } from "../utils/formatDate"

export function RunEntryCard({ run, selected, onToggle }: {
    run: RunSummary
    selected: boolean
    onToggle: () => void
}) {
    return (
        <button
            type="button"
            onClick={onToggle}
            className={`relative flex flex-col text-left border rounded-lg p-lg transition-colors ${selected
                ? "border-primary bg-secondary-container"
                : "border-outline-variant bg-surface-container-lowest hover:border-primary"
                }`}
        >
            <input
                type="checkbox"
                checked={selected}
                readOnly
                className="absolute top-md right-md w-4 h-4 accent-primary pointer-events-none"
            />
            <h3 className="font-headline text-headline-md text-primary pr-lg break-words">{run.name}</h3>
            <p className="font-body text-body-md text-secondary">{run.author}</p>
            <p className="font-body text-label-md text-secondary mt-sm">{run.highlightCount} highlights</p>
            <p className="font-body text-label-sm text-secondary">
                {formatDateTime(run.lastModified)}
            </p>
        </button>
    )
}
