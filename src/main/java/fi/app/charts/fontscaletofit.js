window.fi_app_charts_FontScaleToFit = function() {

    var self = this;
    this.minFontSize = 0.1;
    this.maxFontSize = 3;

    // Get the label element
    var connectorId = this.getParentId();
    this.labelElement = this.getElement(connectorId);

    this.wrapToDiv = function(labelDiv) {
        var wrapper = document.createElement('div');
        wrapper.className = "font-scale";
        wrapper.innerHTML = labelDiv.innerHTML;
        wrapper.style.margin = "0";
        wrapper.style.padding = "0";
        wrapper.style.overflow = "visible";
        labelDiv.innerHTML = "";
        return labelDiv.appendChild(wrapper);
    };

    this.rescaleFontToFit = function() {
        // Check for changes
        if (self.scaledWidth !== self.element.offsetWidth ||
                self.scaledHeight !== self.element.offsetHeight) {
            // Delay calculation
            setTimeout(function() {
                self.doScale(self.minFontSize, self.maxFontSize);
            }, 1);
        }
    };

    this.scaleFontToFit = function(minFontSizePx, maxFontSizePx) {
        self.minFontSize = minFontSizePx;
        self.maxFontSize = maxFontSizePx;
        //self.rescaleFontToFit();
    };

    this.doScale = function(minFontSizePx, maxFontSizePx) {

        var el = self.element;
        var l = self.labelElement;
        var styles = getComputedStyle(el);
        var direction = -1; // default: scale down
        var delta = 2; // 2 px at time

        // Use current size as starting point
        el.style.fontSize = styles.fontSize;
        el.style.lineHeight = styles.lineHeight;

        // Determine direction
        direction = (el.offsetWidth > l.clientWidth ||
                el.offsetHeight > l.clientHeight) ?
                -1 : 1;

        var newFontSize = parseInt(el.style.fontSize);
        var newLineHeight = parseInt(el.style.fontSize);
        while ((direction < 0 && newFontSize > minFontSizePx && (el.offsetWidth > l.clientWidth ||
                el.offsetHeight > l.clientHeight))
                ||
                (direction > 0 && newFontSize < maxFontSizePx && (el.offsetWidth < l.clientWidth ||
                        el.offsetHeight < l.clientHeight))) {

            // Calculate new font size
            newFontSize = parseInt(el.style.fontSize) + (direction * delta);
            newLineHeight = parseInt(el.style.lineHeight) + (direction * delta);


            // Try new font size
            el.style.fontSize = newFontSize + "px";
            el.style.lineHeight = newLineHeight + "px";
        }

        // Use last "ok values"
        el.style.fontSize = (newFontSize - (direction * delta)) + "px";
        el.style.lineHeight = (newLineHeight - (direction * delta)) + "px";

        // Store for rescale check
        self.scaledWidth = el.offsetWidth;
        self.scaledHeight = el.offsetHeight;
    };

    this.element = this.wrapToDiv(this.labelElement);
    this.addResizeListener(this.labelElement, self.rescaleFontToFit);

};
