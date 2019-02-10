(ns com.ben-allred.letshang.common.views.dashboard
  (:require
    [com.ben-allred.letshang.common.utils.dates :as dates]
    [com.ben-allred.letshang.common.utils.logging :as log]
    [com.ben-allred.letshang.common.views.auth :as auth]
    [com.ben-allred.letshang.common.views.components :as components]))

(def ^:private ideas
  ["Barbecue"
   "Girl's Night"
   "Laser Tag Tournament"
   "Super Bowl Party"
   "Wine Tasting"
   "Block Party"
   "LARPing Weekend"
   "Staring Contest"
   "Brunch"
   "Group Yoga"
   "Tea Party"
   "Bat Mitzvah"
   "Happy Hour"
   "Game Night"
   "School Reunion"
   "Birthday Party"
   "Arts & Crafts Meet-up"
   "Pumpkin Carving Event"
   "Canned Food Drive"
   "Zombie Apocalypse Training Seminar"
   "Beach Vacation"
   "Family Fun Time"
   "Spa Day"
   "Halloween Costume Party"
   "Camping Trip"
   "Study Session"
   "Band Practice"
   "Three-Legged Relay Race"
   "Yard Sale"
   "Science Fiction Convention"
   "Book Club Gathering"
   "Car Wash Fund Raiser"
   "Juggling Competition"])

(defn ^:private jumbotron []
  [:div.jumbotron
   [:div.background-image]
   [:div.logo-wrapper
    [:img.logo
     {:src "/images/logo.png"}]]
   [:div.tag-line.space-below
    [:p "Organizing a get together"]
    [:p "shouldn't be a chore."]
    [auth/login "login to get started" "skuttleman@gmail.com"]]])

(defn ^:private tiles []
  [:div.gutters.xl.xxl
   [:div.tile.is-parent
    [:div.tile.is-parent
     [:article.tile.is-child.notification
      [:div.dashboard-tile
       [:div
        [:h1.title [:i.fas.fa-users] " Who?"]
        [:h2.subtitle "Limit the invitees or welcome friends of friends"]
        [:p "Do you want to do brunch with a select few friends? Are your co-workers welcome to invite other people to
          happy hour? Maybe there's room for extra people at your Talk Like a Pirate Day gala, but you want to have
          final say in who can come. No problem. The only rule is: there are no rules (with the exception of a few
          rules)."]]
       [:div.get-started
        [:div [auth/login "get started" "skuttleman@gmail.com"]]]]]]
    [:div.tile.is-parent
     [:article.tile.is-child.notification
      [:div.dashboard-tile
       [:div
        [:h1.title [:i.fas.fa-calendar-alt] " When?"]
        [:h2.subtitle "Specify the date and time or coordinate with everyone's schedule"]
        [:p "Coordinating people's schedules can be challenging, but it shouldn't be a chore. Everyone can indicate what
          works best for them. Decide democratically, or limit it to the guest of honor's availability. Plans change.
          People accidentally double-book. Adapt as necessary."]]
       [:div.get-started
        [:div [auth/login "get started" "skuttleman@gmail.com"]]]]]]
    [:div.tile.is-parent
     [:article.tile.is-child.notification
      [:div.dashboard-tile
       [:div
        [:h1.title [:i.fas.fa-map-marked-alt] " Where?"]
        [:h2.subtitle "Pick the place or let others make suggestions and vote on it"]
        [:p "Whether you know where you want to meet or you're hoping one of your friends will step up and volunteer to
          host, planning a get-together shouldn't be difficult. Suggest a few options. Let your friends make
          suggestions. Then see where people prefer. Or just pick your favorite place. It's your shindig. It's your
          choice."]]
       [:div.get-started
        [:div [auth/login "get started" "skuttleman@gmail.com"]]]]]]]])

(defn ^:private event-types [items]
  [:div.gutters.xl.xxl.space-below
   [:div.inset
    [:div.inset
     [:article.has-text-centered
      [:div.dashboard-tile
       [:div
        [:h1.title "Plan your next:"]
        [:div.idea-container
         [:div.idea-fade]
         [:ul.ideas
          (for [[classes idea] items]
            ^{:key idea} [:li.idea
                          [:h2.subtitle
                           {:class classes}
                           idea]])]]]
       [:div.get-started
        [:div [auth/login-as "login to get started"]]]]]]]])

(defn footer []
  [:footer.footer
   [:div.content.has-text-centered
    "Copyright " [components/unicode :©] (dates/year (dates/now))]])

(defn root [_state]
  [:div.page-dashboard
   [jumbotron]
   [tiles]
   [components/auto-scroller event-types ideas 8 1000]
   [footer]])
