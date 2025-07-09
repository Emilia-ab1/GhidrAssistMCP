/* 
 * 
 */
package ghidrassistmcp.tools;

import java.util.List;
import java.util.Map;

import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.DataIterator;
import ghidra.program.model.listing.Program;
import ghidrassistmcp.McpTool;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * MCP tool that lists string data found in the program.
 */
public class ListStringsTool implements McpTool {
    
    @Override
    public String getName() {
        return "list_strings";
    }
    
    @Override
    public String getDescription() {
        return "List string data found in the program";
    }
    
    @Override
    public McpSchema.JsonSchema getInputSchema() {
        return new McpSchema.JsonSchema("object", 
            Map.of(
                "offset", new McpSchema.JsonSchema("integer", null, null, null, null, null),
                "limit", new McpSchema.JsonSchema("integer", null, null, null, null, null),
                "min_length", new McpSchema.JsonSchema("integer", null, null, null, null, null)
            ),
            List.of(), null, null, null);
    }
    
    @Override
    public McpSchema.CallToolResult execute(Map<String, Object> arguments, Program currentProgram) {
        if (currentProgram == null) {
            return McpSchema.CallToolResult.builder()
                .addTextContent("No program currently loaded")
                .build();
        }
        
        // Parse optional parameters
        int offset = 0;
        int limit = 100; // Default limit
        int minLength = 4; // Default minimum string length
        
        if (arguments.get("offset") instanceof Number) {
            offset = ((Number) arguments.get("offset")).intValue();
        }
        if (arguments.get("limit") instanceof Number) {
            limit = ((Number) arguments.get("limit")).intValue();
        }
        if (arguments.get("min_length") instanceof Number) {
            minLength = ((Number) arguments.get("min_length")).intValue();
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Strings in program (min length: ").append(minLength).append("):\n\n");
        
        DataIterator dataIter = currentProgram.getListing().getDefinedData(true);
        
        int count = 0;
        int totalCount = 0;
        
        while (dataIter.hasNext()) {
            Data data = dataIter.next();
            
            // Check if this is string data
            if (data.hasStringValue()) {
                String stringValue = data.getDefaultValueRepresentation();
                
                // Apply minimum length filter
                if (stringValue != null && stringValue.length() >= minLength) {
                    totalCount++;
                    
                    // Apply offset
                    if (totalCount <= offset) {
                        continue;
                    }
                    
                    // Apply limit
                    if (count >= limit) {
                        break;
                    }
                    
                    // Clean up the string representation for display
                    String displayString = stringValue;
                    if (displayString.length() > 80) {
                        displayString = displayString.substring(0, 77) + "...";
                    }
                    
                    result.append("@ ").append(data.getAddress())
                          .append(" (").append(stringValue.length()).append(" chars): ")
                          .append(displayString)
                          .append("\n");
                    
                    count++;
                }
            }
        }
        
        if (totalCount == 0) {
            result.append("No strings found in the program with minimum length ").append(minLength).append(".");
        } else {
            result.append("\nShowing ").append(count).append(" of ").append(totalCount).append(" strings");
            if (offset > 0) {
                result.append(" (offset: ").append(offset).append(")");
            }
        }
        
        return McpSchema.CallToolResult.builder()
            .addTextContent(result.toString())
            .build();
    }
}