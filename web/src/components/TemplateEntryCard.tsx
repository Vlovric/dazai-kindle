import { useNavigate } from "react-router-dom"
import type { TemplateSummary } from "../dto/templates"
import { formatDateTime } from "../utils/formatDate"

export function TemplateEntryCard({ template, selected, onToggle }: {
    template: TemplateSummary
    selected: boolean
    onToggle: () => void
}) {
    const navigate = useNavigate()

    return (
        <div
            role="button"
            tabIndex={0}
            onClick={() => navigate(`/templates/${encodeURIComponent(template.name)}`)}
            onKeyDown={(e) => e.key === "Enter" && navigate(`/templates/${encodeURIComponent(template.name)}`)}
            className={`relative flex flex-col text-left border rounded-lg p-lg transition-colors cursor-pointer ${selected
                ? "border-primary bg-secondary-container"
                : "border-outline-variant bg-surface-container-lowest hover:border-primary"
                }`}
        >
            <input
                type="checkbox"
                checked={selected}
                onChange={onToggle}
                onClick={(e) => e.stopPropagation()}
                className="absolute top-md right-md w-4 h-4 accent-primary"
            />
            <h3 className="font-headline text-headline-md text-primary pr-lg break-words">{template.name}</h3>
            <p className="font-body text-label-md text-secondary mt-sm">{template.type === "heading" ? "Heading template" : "Output template"}</p>
            <p className="font-body text-label-sm text-secondary">
                {formatDateTime(template.lastModified)}
            </p>
        </div>
    )
}
