(ns com.ben-allred.letshang.ui.app
  (:require
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.utils.dom :as dom]
    [com.ben-allred.letshang.common.utils.logging :as log :include-macros true]
    [com.ben-allred.letshang.common.views.core :as views]
    [reagent.core :as r]
    com.ben-allred.letshang.ui.services.navigation
    com.ben-allred.letshang.ui.services.ws))

(enable-console-print!)

(defn ^:private app []
  [views/app (store/get-state)])

(defn ^:export mount! []
  (r/render
    [app]
    (.getElementById dom/document "app")))
