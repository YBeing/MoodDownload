import { NavLink } from "react-router-dom";
import { navigationItems } from "@/shared/constants/navigation";

export function Sidebar() {
  const primaryItems = navigationItems.filter((item) => item.key !== "settings");
  const settingsItem = navigationItems.find((item) => item.key === "settings");

  return (
    <aside className="sidebar">
      <nav className="sidebar-nav">
        <div className="sidebar-nav__group">
          {primaryItems.map((item) => (
            <NavLink
              className={({ isActive }) =>
                isActive ? "sidebar-link sidebar-link--active" : "sidebar-link"
              }
              key={item.key}
              to={item.path}
            >
              <span className="sidebar-link__icon">{item.icon}</span>
              <span className="sidebar-link__label">{item.label}</span>
            </NavLink>
          ))}
        </div>
      </nav>

      {settingsItem ? (
        <NavLink
          className={({ isActive }) =>
            isActive ? "sidebar-link sidebar-link--active" : "sidebar-link"
          }
          to={settingsItem.path}
        >
          <span className="sidebar-link__icon">{settingsItem.icon}</span>
          <span className="sidebar-link__label">{settingsItem.label}</span>
        </NavLink>
      ) : null}
    </aside>
  );
}
