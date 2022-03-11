package labsim.model.decisions;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 *
 * CLASS TO MANAGE INTERACTIONS BETWEEN THE IO GRIDS AND THE LOCAL FILE SYSTEM
 *
 */
public class ManagerFileGrids {


    /**
     * METHOD TO READ IN DATA TO GRIDS
     *
     * @param grids refers to the look-up table that stores IO solutions (the 'grids')
     *
     * THE MANAGER IS ACCESSED FROM ManagerPopulateGrids
     */
    public static void read(Grids grids) {

        // read in value_function
        try {
            readwrite(grids.value_function, "read", Parameters.grids_directory, "value_function.uft");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // read in consumption
        try {
            readwrite(grids.consumption, "read", Parameters.grids_directory, "consumption.uft");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (grids.employment1!=null) {
            // read in employment 1
            try {
                readwrite(grids.employment1, "read", Parameters.grids_directory, "employment1.uft");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (grids.employment2!=null) {
            // read in employment 2
            try {
                readwrite(grids.employment2, "read", Parameters.grids_directory, "employment2.uft");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * METHOD TO WRITE DATA TO GRIDS
     *
     * @param grids refers to the look-up table that stores IO solutions (the 'grids')
     *
     * THE MANAGER IS ACCESSED FROM ManagerPopulateGrids
     */
    public static void write(Grids grids) {

        // write value_function
        try {
            readwrite(grids.value_function, "write", Parameters.grids_directory, "value_function.uft");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // write consumption
        try {
            readwrite(grids.consumption, "write", Parameters.grids_directory, "consumption.uft");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (grids.employment1!=null) {
            // write employment 1
            try {
                readwrite(grids.employment1, "write", Parameters.grids_directory, "employment1.uft");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (grids.employment2!=null) {
            // write employment 2
            try {
                readwrite(grids.employment2, "write", Parameters.grids_directory, "employment2.uft");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * METHOD TO READ/WRITE BETWEEN A GRID OBJECT AND A SYSTEM FILE
     *
     * @param grid object to write to / read from
     * @param method string = "read" for reading, and write otherwise
     * @param directory directory of file to interact with
     * @param file_name name of file to interact with
     * @throws IOException exception encountered while executing read/write routine
     */
    public static void readwrite(Grid grid, String method, String directory, String file_name) throws IOException {

        // initialise file reference
        String file_path = directory + file_name;
        if (method.equals("write")) {
            validateDirectory(directory);
            safeDelete(file_path);
        } else {
            if (!validateFileExists(file_path)) throw new IOException("file not found");
        }
        RandomAccessFile file;

        // set-up references for MappedByteBuffer
        final long MAX_BUFFER_BYTES = Integer.MAX_VALUE;
        long total_vals_to_rw = grid.size;
        int max_vals_per_partition = (int)((double)MAX_BUFFER_BYTES / (double)8);
        int number_of_partitions = 1 + (int)(total_vals_to_rw / max_vals_per_partition);

        // loop over buffer partitions
        long position = 0;
        long vals_this_partition;
        for (int ii=0; ii<number_of_partitions; ii++) {
            file = new RandomAccessFile(file_path, "rw");
            if (ii == number_of_partitions-1) {
                vals_this_partition = total_vals_to_rw%max_vals_per_partition;
            } else {
                vals_this_partition = max_vals_per_partition;
            }
            FileChannel file_channel = file.getChannel();
            MappedByteBuffer file_buffer = file_channel.map(FileChannel.MapMode.READ_WRITE, position, 8 * vals_this_partition);
            for (long jj=max_vals_per_partition*ii; jj<max_vals_per_partition*ii+vals_this_partition; jj++) {
                if (method.equals("read")) {
                    grid.put(jj, file_buffer.getDouble());
                } else {
                    file_buffer.putDouble(grid.get(jj));
                }
            }
            position += 8 * vals_this_partition;
            file_channel.close();
            safeClose(file);
        }
    }

    /**
     * METHOD TO CLOSE FILE
     * @param file File object to close
     */
    private static void safeClose(RandomAccessFile file) throws IOException {
        if (file != null) {
            file.close();
        }
    }

    /**
     * METHOD TO CLEAR ANY EXISTING FILE
     * @param file_path full path of file to delete if it exists
     */
    private static void safeDelete(String file_path) {
        File file = new File(file_path);
        try { Files.deleteIfExists(file.toPath());
        } catch (IOException ignored) {
        }
    }

    /**
     * METHOD TO ENSURE THAT DIRECTORY EXISTS
     * @param directory full path of file to delete if it exists
     */
    private static void validateDirectory(String directory) {
        Path path = Paths.get(directory);
        if (!Files.isDirectory(path)) {
            new File(directory).mkdirs();
        }
    }

    /**
     * METHOD TO CLEAR ANY EXISTING FILE
     * @param file_path full path of file to delete if it exists
     * @return boolean true if file exists
     */
    private static boolean validateFileExists(String file_path) {
        File file = new File(file_path);
        return file.exists();
    }
}
