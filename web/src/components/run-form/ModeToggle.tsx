export function ModeToggle<T extends string>({ options, value, onChange }: {
    options: { value: T; label: string }[]
    value: T
    onChange: (value: T) => void
}) {
    return (
        <div className="inline-flex bg-surface-container-high rounded-lg p-xs mb-sm">
            {options.map((opt) => (
                <button
                    key={opt.value}
                    type="button"
                    onClick={() => onChange(opt.value)}
                    className={`px-md py-xs rounded-md text-label-md font-body transition-colors ${value === opt.value
                        ? "bg-surface-container-lowest border border-outline-variant text-primary"
                        : "text-secondary"
                        }`}
                >
                    {opt.label}
                </button>
            ))}
        </div>
    )
}
