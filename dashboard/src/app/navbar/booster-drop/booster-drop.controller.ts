/*
 * Copyright (c) 2015-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
'use strict';
import {ICheDropZoneEventObject} from './booster-drop.directive';

/**
 * @ngdoc controller
 * @author Florent Benoit
 */
export class BoosterDropController {

  static $inject = ['$scope', 'lodash', '$q', '$window', 'proxySettings', 'userDashboardConfig'];

  userDashboardConfig: any;
  proxySettings: string;
  $window: ng.IWindowService;
    $q: ng.IQService;
  $scope: ng.IScope;
  lodash: any;
  HOVER_KO_CLASS = 'booster-drop-hover-ko';
  HOVER_OK_CLASS = 'booster-drop-hover-ok';
  errorMessage: string = null;
  dropClass: string;
  waitingDrop: boolean;
  progressUploadPercent: number;

  /**
   * @ngInject for Dependency injection
   */
  constructor($scope: ng.IScope, lodash: any, $q : ng.IQService, $window: ng.IWindowService, proxySettings : string, userDashboardConfig: any) {
    this.$scope = $scope;
    this.lodash = lodash;
    this.$q = $q;
    this.$window = $window;
    this.proxySettings = proxySettings;
    this.userDashboardConfig = userDashboardConfig;
  }

  dropCallback(evt: ICheDropZoneEventObject): void {
    evt.stopPropagation();
    evt.preventDefault();

    const url = evt.dataTransfer.getData('URL');

    if (url === null || !url.startsWith('https://github.com/')) {
      this.$scope.$apply(() => {
        this.dropClass = this.HOVER_KO_CLASS;
        this.errorMessage = 'invalid URL (not github)';
      });
      return;
    }

    // fixme
    console.log('Dropped URL is', url);


    this.handleUrl(url);
  }

  /**
   * Handle the url during the drop
   * @param url
   */
  handleUrl(url: string): void {


    // promise
    let acceptPromise = this.$q.defer().promise;

    // waiting answer
    this.$scope.$apply(() => {
      this.waitingDrop = true;
    });

    let link: string;
    if (this.userDashboardConfig.developmentMode) {
      link = this.proxySettings;
    } else {
      link = this.$window.location.protocol + '//' + this.$window.location.host;
    }
    link +=  '/f?booster=' + url;

    console.log('perform the redirect...', link);

    this.$window.location.href = link;

    acceptPromise.then(() => {
      this.waitingDrop = false;
      this.dropClass = '';
    }, (error: any) => {
      this.waitingDrop = false;
      this.dropClass = this.HOVER_KO_CLASS;
      if (error.data && error.data.message) {
        this.errorMessage = error.data.message;
      } else {
        this.errorMessage = error;
      }
    });

  }

  dragoverCallback(evt: ICheDropZoneEventObject): void {
    evt.stopPropagation();
    evt.preventDefault();
    let ok = evt.dataTransfer && evt.dataTransfer && evt.dataTransfer.types && this.lodash.indexOf(evt.dataTransfer.types, 'text/uri-list') >= 0;

    const url = evt.dataTransfer.getData('URL');
    if (url == null) {
      ok = false;
    }

    this.$scope.$apply(() => {
      if (ok) {
        this.errorMessage = '';
        this.dropClass = this.HOVER_OK_CLASS;
      } else {
        this.dropClass = this.HOVER_KO_CLASS;
      }
    });
  }

  dragEnterCallback(evt: ICheDropZoneEventObject): void {
    this.cleanup(evt);
  }

  dragLeaveCallback(evt: ICheDropZoneEventObject): void {
    this.cleanup(evt);
  }

  cleanup(evt: ICheDropZoneEventObject): void {
    evt.stopPropagation();
    evt.preventDefault();
    this.$scope.$apply(() => {
      this.waitingDrop = false;
      this.dropClass = '';
      this.errorMessage = '';
    });
  }

}

