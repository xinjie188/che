import { BoosterDropController } from './booster-drop.controller';

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

export interface ICheDropZoneEventObject extends JQueryEventObject {
  dataTransfer: {
    files: any[];
    getData: (key: string) => string;
    types: string[];
  };
  lengthComputable: boolean;
  loaded: number;
  total: number;
}

/**
 * @author Florent Benoit
 */
export class BoosterDrop implements ng.IDirective {

  restrict = 'E';
  transclude = true;
  bindToController = true;
  replace = true;

  controller = 'BoosterDropController';
  controllerAs = 'boosterDropController';

  scope = {

  };

  /**
   * Template for the current drop zone
   * @returns {string} the template
   */
  template (): string {
    const template = '<div class="booster-drop" ng-class="boosterDropController.dropClass" flex layout="column" layout-align="center center">'
      + '<div>Drop Booster <i class="fa fa-rocket"></i></div>'
      + '{{boosterDropController.errorMessage}}'
      + '<div ng-show="boosterDropCtrl.errorMessage.length > 0">{{boosterDropController.errorMessage}}</div>'
      + '<md-progress-circular ng-show="boosterDropController.waitingDrop" md-theme="chesave"  md-mode="indeterminate">'
      + '</md-progress-circular></div>';
    return template;
  }

  /**
   * Keep reference to the model controller
   */
  link($scope: ng.IScope, $element: ng.IAugmentedJQuery, $attributes: ng.IAttributes, $controller: BoosterDropController) {
    let innerElement = $element[0];

    innerElement.addEventListener('dragenter', (evt: ICheDropZoneEventObject) =>  {
      $controller.dragEnterCallback(evt);
    });

    innerElement.addEventListener('dragleave', (evt: ICheDropZoneEventObject) =>  {
      $controller.dragLeaveCallback(evt);
    });
    innerElement.addEventListener('dragover', (evt: ICheDropZoneEventObject) =>  {
      $controller.dragoverCallback(evt);
    });

    innerElement.addEventListener('drop', (evt: ICheDropZoneEventObject) =>  {
      $controller.dropCallback(evt);
    });
  }

}
