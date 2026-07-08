export function SelectionActionBar({ count, busy, onClear, onDelete, onExport }: {
    count: number
    busy?: boolean
    onClear: () => void
    onDelete: () => void
    onExport: () => void
}) {
    return (
        <div className="flex items-center gap-md bg-surface-container-lowest border border-outline-variant rounded-lg px-lg py-md mt-md">
            <span className="font-body text-label-md text-on-surface">{count} {count === 1 ? "file" : "files"} selected</span>
            <button
                type="button"
                onClick={onClear}
                className="font-body text-label-md text-secondary hover:text-primary transition-colors"
            >
                Clear
            </button>
            <div className="flex-1" />
            <button
                type="button"
                disabled={busy}
                onClick={onDelete}
                className="border border-error text-error rounded-lg px-md py-sm text-label-md font-body hover:bg-error-container transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
                Delete
            </button>
            <button
                type="button"
                disabled={busy}
                onClick={onExport}
                className="bg-primary text-on-primary rounded-lg px-md py-sm text-label-md font-body disabled:opacity-50 disabled:cursor-not-allowed"
            >
                Export
            </button>
        </div>
    )
}
