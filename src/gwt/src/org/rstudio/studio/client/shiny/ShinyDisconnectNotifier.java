/*
 * ShinyDisconnectNotifier.java
 *
 * Copyright (C) 2009-19 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.shiny;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;

public class ShinyDisconnectNotifier
{
   public interface ShinyDisconnectSource
   {
      public String getShinyUrl();
      public String getWindowName();
      public void onShinyDisconnect();
   }

   public ShinyDisconnectNotifier(ShinyDisconnectSource source)
   {
      source_ = source;
      suppressUrl_ = null;
      initializeEvents();
   }
   
   /**
    * Begins suppressing disconnect notifications from the current URL.
    */
   public void suppress()
   {
      if (!StringUtil.isNullOrEmpty(suppressUrl_))
      {
         // should never happen in practice; if it does the safest behavior is
         // to respect the new suppress URL and discard the old one. warn that
         // we're doing this.
         Debug.logWarning("Replacing old Shiny disconnect suppress URL: " + suppressUrl_);
      }
      suppressUrl_ = source_.getShinyUrl();
   }
   
   /**
    * Ends suppression of disconnect notifications.
    */
   public void unsuppress()
   {
      suppressUrl_ = null;
   }

   private native void initializeEvents() /*-{  
      
      var self = this;
      var callback = $entry(function(event) {
         
         if (typeof event.data !== "string")
            return;
         
         self.@org.rstudio.studio.client.shiny.ShinyDisconnectNotifier::onMessage(*)(
            e.data,
            e.origin,
            e.target.name
         );
         
      });
      
      $wnd.addEventListener("message", callback, true);
      
   }-*/;
   
   private void onMessage(String data, String origin, String name)
   {
      // check to see whether we should handle this message
      boolean ok =
            StringUtil.equals(data, "disconnected") &&
            StringUtil.equals(name, source_.getWindowName()) &&
            source_.getShinyUrl().startsWith(origin);
      
      if (!ok)
         return;
      
      if (StringUtil.equals(source_.getShinyUrl(), suppressUrl_))
      {
         // we were suppressing disconnect notifications from this URL;
         // consume this disconnection and resume
         unsuppress();
      }
      else
      {
         source_.onShinyDisconnect();
      }
      
   }

   private final ShinyDisconnectSource source_;
   private String suppressUrl_;
}
