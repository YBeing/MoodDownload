import { Navigate, createHashRouter } from "react-router-dom";
import { AppShell } from "@/app/layout/AppShell";
import { RunningPage } from "@/domains/task/pages/RunningPage";
import { CompletedPage } from "@/domains/task/pages/CompletedPage";
import { PausedPage } from "@/domains/task/pages/PausedPage";
import { FailedPage } from "@/domains/task/pages/FailedPage";
import { SettingsPage } from "@/domains/config/pages/SettingsPage";

export const router = createHashRouter([
  {
    path: "/",
    element: <AppShell />,
    children: [
      {
        index: true,
        element: <Navigate to="/tasks/running" replace />
      },
      {
        path: "/tasks/running",
        element: <RunningPage />
      },
      {
        path: "/tasks/completed",
        element: <CompletedPage />
      },
      {
        path: "/tasks/paused",
        element: <PausedPage />
      },
      {
        path: "/tasks/failed",
        element: <FailedPage />
      },
      {
        path: "/settings",
        element: <SettingsPage />
      }
    ]
  }
]);
