(ns com.ben-allred.letshang.common.views.pages.hangouts.responses
  (:require
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.components.core :as components]
    [com.ben-allred.letshang.common.views.components.fields :as fields]
    [com.ben-allred.letshang.common.views.components.loading :as loading]
    [com.ben-allred.letshang.common.views.resources.hangouts.responses :as res.responses]))

(defn icon
  ([response]
   (icon response nil))
  ([response amount]
   (let [icon (res.responses/response->icon response)]
     (cond->> [:span.tag.is-rounded.layout--space-between
               {:class [(res.responses/response->level response)]
                :style {:text-transform :lowercase}}
               (if icon
                 [components/icon {:class ["is-small"]} icon]
                 (res.responses/response->text response))
               (when amount
                 [:span amount])]
              icon (conj [components/tooltip
                          {:text     (res.responses/response->text response)
                           :position :right}])))))

(defn form [response-type {:keys [id response]}]
  (let [form (res.responses/form response-type {:id id :response response :user-id (:id @store/user)})
        unsubscribe (store/subscribe #{:suggestions.when/success} (res.responses/sub form))]
    (r/create-class
      {:component-will-unmount
       (fn [_]
         (unsubscribe))
       :reagent-render
       (fn [_response-type _response]
         [:div.layout--space-between.layout--align-center
          [fields/button-group
           (-> {:class        ["is-small"]
                :label        (res.responses/response-label response-type)
                :label-small? true}
               (res.responses/with-attrs form [:response]))
           (res.responses/response-options response-type)]
          (when-not (forms/ready? form)
            [loading/spinner])])})))
