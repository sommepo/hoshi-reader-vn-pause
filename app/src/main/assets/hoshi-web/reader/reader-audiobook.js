(function() {
  var reader = window.hoshiReader;
  var baseHighlightSasayakiCue = reader.highlightSasayakiCue;
  var USER_SCROLL_HOLD_MS = 2500;
  var MAX_ANIMATED_VIEWPORTS = 3;

  reader.audiobookMode = true;
  // The transport bar covers this much of the bottom of the page until native code corrects it.
  reader.audiobookBottomInset = __HOSHI_AUDIOBOOK_BOTTOM_INSET__;
  reader.audiobookUserScrollAt = 0;
  reader.audiobookTouchActive = false;
  reader.audiobookDidCenter = false;
  reader.audiobookTotalChars = null;
  document.documentElement.setAttribute('data-hoshi-audiobook', 'true');
  document.documentElement.style.scrollPaddingBottom = reader.audiobookBottomInset + 'px';

  reader.audiobookFocusY = function() {
    return Math.max(1, window.innerHeight - this.audiobookBottomInset) / 2;
  };
  reader.progressViewportEdge = function() {
    return this.audiobookFocusY();
  };
  reader.restoreScrollBlock = function() {
    return 'center';
  };
  reader.audiobookScrollTop = function() {
    var root = document.scrollingElement || document.documentElement;
    var top = root.scrollTop;
    if (top === 0 && window.scrollY !== 0) top = window.scrollY;
    return top;
  };
  reader.audiobookMaxScrollTop = function() {
    var root = document.scrollingElement || document.documentElement;
    return Math.max(0, root.scrollHeight - window.innerHeight);
  };
  reader.audiobookCueBounds = function(cueId) {
    var targets = this.isEInkMode()
      ? (this.cueGeometryRanges.get(cueId) || [])
      : this.sasayakiInlineTargetsForCue(cueId);
    var top = Infinity;
    var bottom = -Infinity;
    for (var i = 0; i < targets.length; i++) {
      var rects = targets[i].getClientRects();
      for (var j = 0; j < rects.length; j++) {
        var rect = rects[j];
        if (rect.width <= 0 || rect.height <= 0) continue;
        top = Math.min(top, rect.top);
        bottom = Math.max(bottom, rect.bottom);
      }
    }
    if (top === Infinity) return null;
    return { top: top, bottom: bottom };
  };
  reader.audiobookScrollTargetForCue = function(cueId) {
    var bounds = this.audiobookCueBounds(cueId);
    if (!bounds) return null;
    var visibleHeight = Math.max(1, window.innerHeight - this.audiobookBottomInset);
    var height = bounds.bottom - bounds.top;
    // A line taller than the visible area keeps its start in view instead of its middle.
    var offset = height >= visibleHeight ? bounds.top - visibleHeight * 0.1 : (bounds.top + bounds.bottom) / 2 - this.audiobookFocusY();
    var target = this.audiobookScrollTop() + offset;
    return Math.min(this.audiobookMaxScrollTop(), Math.max(0, target));
  };
  reader.audiobookUserIsScrolling = function() {
    return this.audiobookTouchActive || (Date.now() - this.audiobookUserScrollAt) < USER_SCROLL_HOLD_MS;
  };
  reader.resumeAudiobookFollow = function() {
    this.audiobookTouchActive = false;
    this.audiobookUserScrollAt = 0;
    if (this.activeCueId) this.centerAudiobookCue(this.activeCueId, true);
  };
  reader.centerAudiobookCue = function(cueId, animated) {
    var target = this.audiobookScrollTargetForCue(cueId);
    if (target === null) return false;
    var current = this.audiobookScrollTop();
    var distance = Math.abs(target - current);
    if (distance < 1) return false;
    var smooth = animated &&
      this.audiobookDidCenter &&
      !this.isEInkMode() &&
      distance <= window.innerHeight * MAX_ANIMATED_VIEWPORTS;
    this.audiobookDidCenter = true;
    window.scrollTo({ left: 0, top: target, behavior: smooth ? 'smooth' : 'instant' });
    if (this.isEInkMode()) {
      var self = this;
      requestAnimationFrame(function() { self.renderSasayakiOverlay(); });
    }
    return true;
  };
  reader.audiobookProgressForCue = function(cue) {
    var start = cue && typeof cue === 'object' ? Number(cue.start) : NaN;
    if (!Number.isFinite(start)) return null;
    if (this.audiobookTotalChars === null) {
      var walker = this.createWalker();
      var total = 0;
      var node;
      while (node = walker.nextNode()) {
        total += this.countChars(node.textContent);
      }
      this.audiobookTotalChars = total;
    }
    if (this.audiobookTotalChars <= 0) return null;
    return Math.min(1, Math.max(0, start / this.audiobookTotalChars));
  };
  reader.setAudiobookBottomInset = function(inset) {
    var value = Number(inset);
    if (!Number.isFinite(value) || value < 0) value = 0;
    if (value === this.audiobookBottomInset) return;
    this.audiobookBottomInset = value;
    document.documentElement.style.scrollPaddingBottom = value + 'px';
    if (this.activeCueId && !this.audiobookUserIsScrolling()) {
      this.centerAudiobookCue(this.activeCueId, false);
    }
  };
  reader.highlightSasayakiCue = function(cue, reveal) {
    baseHighlightSasayakiCue.call(this, cue, false);
    var cueId = typeof cue === 'string' ? cue : cue && cue.id;
    if (!cueId || this.activeCueId !== cueId || !reveal) return null;
    if (!this.audiobookUserIsScrolling()) {
      this.centerAudiobookCue(cueId, true);
    }
    return this.audiobookProgressForCue(cue);
  };

  var passive = { passive: true };
  document.addEventListener('touchstart', function() {
    reader.audiobookTouchActive = false;
  }, passive);
  document.addEventListener('touchmove', function() {
    reader.audiobookTouchActive = true;
    reader.audiobookUserScrollAt = Date.now();
  }, passive);
  var endTouch = function() {
    if (reader.audiobookTouchActive) reader.audiobookUserScrollAt = Date.now();
    reader.audiobookTouchActive = false;
  };
  document.addEventListener('touchend', endTouch, passive);
  document.addEventListener('touchcancel', endTouch, passive);
})();
