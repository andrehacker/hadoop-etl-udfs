package com.exasol.hadoop.hcat;

import com.exasol.hadoop.WebHdfsAndHCatService;
import com.exasol.hadoop.hive.HiveMetastoreService;
import com.exasol.hadoop.kerberos.KerberosCredentials;
import com.exasol.hadoop.kerberos.KerberosHadoopUtils;
import org.apache.hadoop.security.UserGroupInformation;

import java.security.PrivilegedExceptionAction;

public class HCatMetadataService {
    
    /**
     * Retrieve the table metadata required for the Hadoop ETL UDFs from HCatalog or webHCatalog
     */
    public static HCatTableMetadata getMetadataForTable(
            final String dbName,
            final String tableName,
            final String hCatAddress,
            final String hCatUser,  // In case of Kerberos, this is the service principle typically of Hive
            final boolean useKerberos,
            final KerberosCredentials kerberosCredentials) throws Exception {
        
        UserGroupInformation ugi = useKerberos ?
                KerberosHadoopUtils.getKerberosUGI(kerberosCredentials) : UserGroupInformation.createRemoteUser(hCatUser);
        HCatTableMetadata tableMeta = ugi.doAs(new PrivilegedExceptionAction<HCatTableMetadata>() {
            public HCatTableMetadata run() throws Exception {
                HCatTableMetadata tableMeta;
                if (hCatAddress.toLowerCase().startsWith("thrift://")) {
                    // Get table metadata via faster native Hive Metastore API
                    tableMeta = HiveMetastoreService.getTableMetadata(hCatAddress, dbName, tableName, useKerberos, hCatUser);
                } else {
                    // Get table metadata from webHCat
                    String responseJson = WebHdfsAndHCatService.getExtendedTableInfo(hCatAddress, dbName, tableName, useKerberos?kerberosCredentials.getPrinciple():hCatUser);
                    tableMeta = WebHCatJsonParser.parse(responseJson);
                }
                return tableMeta;
            }
        });
        return tableMeta;
    }
}
