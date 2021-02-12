package com.github.solarbeam.corona19out

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class Koroutine {
    companion object {
        fun BackgroundTask(context: Context) {
            //onPreExecute
            CoroutineScope(Dispatchers.Main).launch {
                //doInBackground
                async(Dispatchers.Default) {
                    DataHandler._testBusan(context)

                }.await();
                //onPostExecute
            }

        }
    }
}