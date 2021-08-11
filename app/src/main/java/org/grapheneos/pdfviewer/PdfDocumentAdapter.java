package org.grapheneos.pdfviewer;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfDocumentAdapter extends PrintDocumentAdapter {

    final Context context;
    final ISSource isSource;

    interface ISSource {
        InputStream getInputStream() throws FileNotFoundException;
    }

    public PdfDocumentAdapter(final Context context, final ISSource isSource) {
        this.context = context;
        this.isSource = isSource;
    }

    @Override
    public void onLayout(PrintAttributes printAttributes, PrintAttributes printAttributes1, CancellationSignal cancellationSignal, LayoutResultCallback layoutResultCallback, Bundle bundle) {
        if (cancellationSignal.isCanceled()) {
            layoutResultCallback.onLayoutCancelled();
        }
        else {
            PrintDocumentInfo.Builder builder=
                    new PrintDocumentInfo.Builder(" file name");
            builder.setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build();
            layoutResultCallback.onLayoutFinished(builder.build(),
                    !printAttributes1.equals(printAttributes));
        }
    }

    @Override
    public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor parcelFileDescriptor, CancellationSignal cancellationSignal, WriteResultCallback writeResultCallback) {
        OutputStream out = null;
        InputStream in = null;
        try {
            out = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
            in = isSource.getInputStream();

            byte[] buf=new byte[16384];
            int size;

            while ((size=in.read(buf)) >= 0
                    && !cancellationSignal.isCanceled()) {
                out.write(buf, 0, size);
            }

            if (cancellationSignal.isCanceled()) {
                writeResultCallback.onWriteCancelled();
            }
            else {
                writeResultCallback.onWriteFinished(new PageRange[] { PageRange.ALL_PAGES });
            }
        }
        catch (Exception e) {
            writeResultCallback.onWriteFailed(e.getMessage());
            Log.d("PDFDocumentAdapter" , e.getMessage());
        }
        finally {
            try {
                if(in!=null) in.close();
                if(out!=null) out.close();
            }
            catch (IOException e) {
                Log.d("PDFDocumentAdapter", e.getMessage());
            }
        }
    }
}