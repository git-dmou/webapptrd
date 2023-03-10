/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

;
(function(undefined) {
	'use strict';

	if (typeof (Wicket) === "undefined") {
		window.Wicket = {};
	}

	Wicket.WUPB = Wicket.Class.create();
	Wicket.WUPB.prototype = {

		initialize : function(formid, statusid, barid, url, fileid,
				initialStatus, endedStatus, hideAfterUpload) {
			this.statusid = statusid;
			this.barid = barid;
			this.url = url;
			this.fileid = fileid;
			this.initialStatus = initialStatus;
			this.endedStatus = endedStatus;
			this.hideAfterUpload = hideAfterUpload;
			console.log("formid=" + formid + " statusid=" + statusid
					+ " barid=" + barid + " url=" + url + " fileid=" + fileid
					+ " initialStatus=" + initialStatus + " endedStatus="
					+ endedStatus + " hideAfterUpload=" + hideAfterUpload);

			var formElement = Wicket.$(formid);
			this.originalCallback = formElement.onsubmit;
			formElement.onsubmit = Wicket.bind(this.submitCallback, this);
		},

		submitCallback : function() {
			if (this.originalCallback && !this.originalCallback()) {
				return false;
			} else {
				this.start();
				return true;
			}
		},

		start : function() {
			var fileupload = Wicket.$(this.fileid);
			if (fileupload && fileupload.value) {
				this.setPercent(0);
				this.setStatus(this.initialStatus);
				Wicket.DOM.show(this.statusid, 'block');
				Wicket.DOM.show(this.barid, 'block');
				this.scheduleUpdate();
			}
		},

		setStatus : function(status) {
			var label = document.createElement("label");
			label.innerHTML = status;
			var oldLabel = Wicket.$(this.statusid).firstChild;
			if (oldLabel != null) {
				Wicket.$(this.statusid).removeChild(oldLabel);
			}
			Wicket.$(this.statusid).appendChild(label);

			if (status == undefined) {
				Wicket.DOM.hide(this.statusid);
			}
		},

		setPercent : function(progressPercent) {
			console.log(progressPercent);
			Wicket.$(this.barid).style.width = progressPercent + '%';
		},

		scheduleUpdate : function() {
			window.setTimeout(Wicket.bind(this.load, this), 500);
		},

		_createIFrame : function(iframeName) {
			var $iframe = jQuery('<iframe name="'
					+ iframeName
					+ '" id="'
					+ iframeName
					+ '" src="about:blank" style="position: absolute; top: -9999px; left: -9999px;">');
			return $iframe[0];
		},

		load : function() {
			var URL = this.url;

			this.iframe = this._createIFrame("" + Math.random());

			document.body.appendChild(this.iframe);

			Wicket.Event.add(this.iframe, "load", Wicket
					.bind(this.update, this));
			this.iframe.src = URL;
		},

		update : function() {
			var responseAsText;
			if (this.iframe.contentDocument) {
				responseAsText = this.iframe.contentDocument.body.innerHTML;
			} else {
				// for IE 5.5, 6 and 7:
				responseAsText = this.iframe.contentWindow.document.body.innerHTML;
			}

			console.log("update : responseAsText=" + responseAsText);
			var update = responseAsText.split('|');

			var progressPercent = update[1];
			var status = update[2];

			this.setPercent(progressPercent);
			this.setStatus(status);

			this.iframe.parentNode.removeChild(this.iframe);
			this.iframe = null;

			if (this.hideAfterUpload == 'true') {
				// Si on voulait masquer la barre de progression et le statut.
				if (progressPercent === '100') {
					Wicket.DOM.hide(this.statusid);
					Wicket.DOM.hide(this.barid);
				} else {
					this.scheduleUpdate();
				}
			} else {
				// On affiche le statut de fin et 100%
				if (progressPercent === '100') {
					this.setPercent(100);
					this.setStatus(this.endedStatus);
				} else {
					this.scheduleUpdate();
				}
			}
		}
	};
})();
