import { NavLink } from "react-router-dom"

const NAV_ITEMS = [
    { to: "/", label: "New Run", end: true },
    { to: "/library", label: "Library" },
    { to: "/templates", label: "Templates" },
    { to: "/paths", label: "Paths" },
]

export function Sidebar() {
    return (
        <aside className="fixed left-0 top-0 h-full w-64 bg-surface-container-lowest border-r border-outline-variant flex flex-col">
            <div className="px-lg py-xl">
                <h1 className="font-headline text-headline-md text-primary">Dazai Kindle</h1>
            </div>
            <nav className="flex-1 px-md flex flex-col gap-sm">
                {NAV_ITEMS.map((item) => (
                    <NavLink
                        key={item.to}
                        to={item.to}
                        end={item.end}
                        className={({ isActive }) =>
                            `px-md py-sm rounded-lg text-label-md font-body transition-colors ${isActive
                                ? "bg-secondary-container text-on-secondary-container"
                                : "text-secondary hover:bg-surface-container-high"
                            }`
                        }
                    >
                        {item.label}
                    </NavLink>
                ))}
            </nav>
        </aside>
    )
}
