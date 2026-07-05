export function RunCard({ title, explanation, onClick }: {
    title: string
    explanation: string
    onClick: () => void
}) {
    return (
        <button
            onClick={onClick}
            className="group flex flex-col text-left bg-surface-container-lowest border border-outline-variant rounded-lg p-lg transition-colors hover:border-primary"
        >
            <h3 className="font-headline text-headline-md text-primary mb-sm">{title}</h3>
            <p className="font-body text-body-md text-secondary flex-1">{explanation}</p>
            <div className="flex items-center gap-xs text-primary text-label-md font-body mt-md">
                <span>Initiate</span>
                <span aria-hidden="true" className="transition-transform group-hover:translate-x-1">&rarr;</span>
            </div>
        </button>
    )
}
