define([
  './models/state',
  './layout',
  './models/issues',
  'components/navigator/models/facets',
  './models/filters',
  './controller',
  './router',
  './workspace-list-view',
  './workspace-header-view',
  './facets-view'
], function (State, Layout, Issues, Facets, Filters, Controller, Router, WorkspaceListView, WorkspaceHeaderView,
             FacetsView) {

  var $ = jQuery,
      App = new Marionette.Application(),
      init = function (options) {
        this.config = options.config;
        this.state = new State({
          isContext: true,
          contextQuery: { componentUuids: options.config.resource },
          contextComponentUuid: options.config.resource,
          contextComponentName: options.config.resourceName,
          contextComponentQualifier: options.config.resourceQualifier
        });
        this.updateContextFacets();
        this.list = new Issues();
        this.facets = new Facets();
        this.filters = new Filters();

        this.layout = new Layout({ app: this, el: options.el });
        this.layout.render();
        $('#footer').addClass('search-navigator-footer');

        this.controller = new Controller({ app: this });

        this.issuesView = new WorkspaceListView({
          app: this,
          collection: this.list
        });
        this.layout.workspaceListRegion.show(this.issuesView);
        this.issuesView.bindScrollEvents();

        this.workspaceHeaderView = new WorkspaceHeaderView({
          app: this,
          collection: this.list
        });
        this.layout.workspaceHeaderRegion.show(this.workspaceHeaderView);

        this.facetsView = new FacetsView({
          app: this,
          collection: this.facets
        });
        this.layout.facetsRegion.show(this.facetsView);

        this.controller.fetchFilters().done(function () {
          key.setScope('list');
          App.router = new Router({ app: App });
          Backbone.history.start();
        });
      };

  App.getContextQuery = function () {
    return { componentUuids: this.config.resource };
  };

  App.getRestrictedFacets = function () {
    return {
      'TRK': ['projectUuids'],
      'BRC': ['projectUuids'],
      'DIR': ['projectUuids', 'moduleUuids', 'directories'],
      'DEV': ['authors'],
      'DEV_PRJ': ['projectUuids', 'authors']
    };
  };

  App.updateContextFacets = function () {
    var facets = this.state.get('facets'),
        allFacets = this.state.get('allFacets'),
        facetsFromServer = this.state.get('facetsFromServer');
    return this.state.set({
      facets: facets,
      allFacets: _.difference(allFacets, this.getRestrictedFacets()[this.config.resourceQualifier]),
      facetsFromServer: _.difference(facetsFromServer, this.getRestrictedFacets()[this.config.resourceQualifier])
    });
  };

  App.on('start', function (options) {
    $.when(window.requestMessages()).done(function () {
      init.call(App, options);
    });
  });

  return App;

});
