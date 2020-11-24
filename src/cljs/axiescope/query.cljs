(ns axiescope.query
  (:require
    [graphql-query.core :refer [graphql-query]]
    ))

(def axie-brief-fragment
  {:fragment/name :fragment/AxieBrief
   :fragment/type :Axie
   :fragment/fields [:id
                     :name
                     :image
                     :class
                     :genes
                     :title
                     :breedCount
                     :level
                     [:stats [:fragment/AxieStats]]
                     [:parts [:fragment/AxiePart]]]})

(def axie-stats-fragment
  {:fragment/name :fragment/AxieStats
   :fragment/type :AxieStats
   :fragment/fields [:hp :speed :skill :morale]})

(def axie-part-fragment
  {:fragment/name :fragment/AxiePart
   :fragment/type :AxiePart
   :fragment/fields [:id
                     :name
                     :class
                     :type
                     :specialGenes ; ie "Mystic"
                     :stage
                     [:abilities [:fragment/AxieCardAbility]]]})

(def axie-card-fragment
  {:fragment/name :fragment/AxieCardAbility
   :fragment/type :AxieCardAbility
   :fragment/fields [:id
                     :name
                     :attack
                     :defense
                     :energy
                     :description
                     :backgroundUrl
                     :effectIconUrl]})

(def fetch-my-axies-query
  (graphql-query
    {:operation {:operation/type :query
                 :operation/name :GetAxieBriefList}
     :variables [{:variable/name :$auctionType
                  :variable/type :AuctionType}
                 {:variable/name :$criteria
                  :variable/type :AxieSearchCriteria}
                 {:variable/name :$from
                  :variable/type :Int}
                 {:variable/name :$sort
                  :variable/type :SortBy}
                 {:variable/name :$size
                  :variable/type :Int}
                 {:variable/name :$owner
                  :variable/type :String}]
     :queries [{:query/data [:axies
                             {:auctionType :$auctionType
                              :criteria :$criteria
                              :from :$from
                              :sort :$sort
                              :size :$size
                              :owner :$owner}
                             [:total
                             [:results [:fragment/AxieBrief]]]]}]
     :fragments [axie-brief-fragment
                 axie-stats-fragment
                 axie-part-fragment
                 axie-card-fragment]}))

(def fetch-axie-query
  (graphql-query
    {:operation {:operation/type :query
                 :operation/name :GetAxieDetail}
     :variables [{:variable/name :$axieId
                  :variable/type :ID!}]
     :queries [{:query/data [:axie
                             {:axieId :$axieId}
                             :fragment/AxieBrief]}]
     :fragments [axie-brief-fragment
                 axie-stats-fragment
                 axie-part-fragment
                 axie-card-fragment]}))
