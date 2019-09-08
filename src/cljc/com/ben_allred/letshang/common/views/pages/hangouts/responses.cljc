(ns com.ben-allred.letshang.common.views.pages.hangouts.responses
  (:require
    [com.ben-allred.letshang.common.resources.hangouts.responses :as res.responses]
    [com.ben-allred.letshang.common.services.forms.core :as forms]
    [com.ben-allred.letshang.common.services.store.core :as store]
    [com.ben-allred.letshang.common.stubs.reagent :as r]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.components.core :as components]
    [com.ben-allred.letshang.common.views.components.fields :as fields]
    [com.ben-allred.letshang.common.views.components.loading :as loading]))

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

(defn form [response-type {model-id :id}]
  (let [form (res.responses/form response-type model-id)
        unsubscribe (store/subscribe #{:suggestions.where/success :suggestions.when/success} (res.responses/sub response-type form))]
    (r/create-class
      {:component-will-unmount
       (fn [_]
         (unsubscribe))
       :reagent-render
       (fn [_response-type _response]
         [:div.layout--space-between.layout--align-center
          [fields/button-group
           (-> {:class        ["is-small"]
                :label        (res.responses/response->label response-type)
                :label-small? true}
               (res.responses/with-attrs form [:response]))
           (res.responses/response->options response-type)]
          (when-not (forms/ready? form)
            [loading/spinner])])})))
