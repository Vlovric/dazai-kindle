import { useTopBar } from "../hooks/useTopBar"

export function TopBar() {
    const { actions } = useTopBar()

    return (
        <header className="flex items-center justify-end gap-md px-lg py-sm border-b border-outline-variant bg-surface min-h-[56px]">
            {actions}
        </header>
    )
}
