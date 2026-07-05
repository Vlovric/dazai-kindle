import { useEffect, type ReactNode } from "react"
import { useTopBar } from "./useTopBar"

/**
 * Pushes content into the shared top bar for as long as the calling page is
 * mounted. `actions` must be a stable reference (wrap it in useMemo, keyed on
 * whatever it actually depends on) - a new JSX literal every render would
 * set off an infinite loop: setActions -> context update -> caller
 * re-renders (it reads the context too) -> new reference -> setActions -> ...
 */
export function useTopBarActions(actions: ReactNode) {
    const { setActions } = useTopBar()

    useEffect(() => {
        setActions(actions)
        return () => setActions(null)
    }, [actions, setActions])
}
