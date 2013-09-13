using Microsoft.Phone.Controls;
using Microsoft.Phone.Notification;
using System;
using System.Diagnostics;
using System.Text;
using System.Threading;
using System.Runtime.Serialization;
using System.Windows;

namespace WPCordovaClassLib.Cordova.Commands
{
    public class PushPlugin : BaseCommand
    {
        private HttpNotificationChannel pushChannel;
        private string channelName;
        private string toastCallback;

        /// <summary>
        /// Register method
        /// </summary>
        /// <param name="options"></param>
        public void register(string options)
        {
            Options pushOptions;

            try
            {
                string[] args = JSON.JsonHelper.Deserialize<string[]>(options);
                pushOptions = JSON.JsonHelper.Deserialize<Options>(args[0]);
                this.channelName = pushOptions.ChannelName;
                this.toastCallback = pushOptions.NotificationCallback;
            }
            catch (Exception)
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
                return;
            }

            pushChannel = HttpNotificationChannel.Find(channelName);
            if (pushChannel == null)
            {
                pushChannel = new HttpNotificationChannel(channelName);

                pushChannel.ChannelUriUpdated += PushChannel_ChannelUriUpdated;
                pushChannel.ErrorOccurred += PushChannel_ErrorOccurred;
                pushChannel.ShellToastNotificationReceived += PushChannel_ShellToastNotificationReceived;

                pushChannel.Open();
                pushChannel.BindToShellToast();
            }
            else
            {
                pushChannel.ChannelUriUpdated += PushChannel_ChannelUriUpdated;
                pushChannel.ErrorOccurred += PushChannel_ErrorOccurred;
                pushChannel.ShellToastNotificationReceived += PushChannel_ShellToastNotificationReceived;

                RegisterResult result = new RegisterResult();
                result.ChannelName = this.channelName;
                result.Uri = pushChannel.ChannelUri.ToString();
                this.DispatchCommandResult(new PluginResult(PluginResult.Status.OK, result));
            }
        }

        /// <summary>
        /// Unregister method 
        /// </summary>
        /// <param name="options"></param>
        public void unregister(string options)
        {

            if (pushChannel != null)
            {
                pushChannel.Close();

                pushChannel.ChannelUriUpdated -= PushChannel_ChannelUriUpdated;
                pushChannel.ErrorOccurred -= PushChannel_ErrorOccurred;
                pushChannel.ShellToastNotificationReceived -= PushChannel_ShellToastNotificationReceived;

                pushChannel.Dispose();
                pushChannel = null;
            }

            this.DispatchCommandResult(new PluginResult(PluginResult.Status.OK));
        }

        void PushChannel_ChannelUriUpdated(object sender, NotificationChannelUriEventArgs e)
        {
            // return uri to js
            RegisterResult result = new RegisterResult();
            result.ChannelName = this.channelName;
            result.Uri = pushChannel.ChannelUri.ToString();
            this.DispatchCommandResult(new PluginResult(PluginResult.Status.OK, result));
        }

        void PushChannel_ErrorOccurred(object sender, NotificationChannelErrorEventArgs e)
        {
            // call error handler and return uri
            RegisterError err = new RegisterError();
            err.Code = e.ErrorCode.ToString();
            err.Message = e.Message;
            this.DispatchCommandResult(new PluginResult(PluginResult.Status.ERROR, err));
        }

        void PushChannel_ShellToastNotificationReceived(object sender, NotificationEventArgs e)
        {
            StringBuilder message = new StringBuilder();
            string relativeUri = string.Empty;

            Toast toast = new Toast();
            toast.Title = e.Collection["wp:Text1"];
            toast.Subtitle = e.Collection["wp:Text2"];
            PluginResult result = new PluginResult(PluginResult.Status.OK, toast);

            var script = new ScriptCallback(this.toastCallback, new string[] { result.Message });

            this.InvokeCustomScript(script, false);
        }

        [DataContract]
        public class Toast
        {
            [DataMember(Name = "text1", IsRequired = false)]
            public string Title { get; set; }

            [DataMember(Name = "text2", IsRequired = false)]
            public string Subtitle { get; set; }
        }

        [DataContract]
        public class Options
        {
            [DataMember(Name = "channelName", IsRequired = true)]
            public string ChannelName { get; set; }

            [DataMember(Name = "ecb", IsRequired = false)]
            public string NotificationCallback { get; set; }
        }

        [DataContract]
        public class RegisterResult
        {
            [DataMember(Name = "uri", IsRequired = true)]
            public string Uri { get; set; }

            [DataMember(Name = "channel", IsRequired = true)]
            public string ChannelName { get; set; }
        }

        [DataContract]
        public class RegisterError
        {
            [DataMember(Name = "code", IsRequired = true)]
            public string Code { get; set; }

            [DataMember(Name = "message", IsRequired = true)]
            public string Message { get; set; }
        }
    }
}